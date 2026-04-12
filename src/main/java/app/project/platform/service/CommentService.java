package app.project.platform.service;

import app.project.platform.domain.PostRedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentLikeRepository;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ContentRepository contentRepository;

    private final MemberRepository memberRepository;

    private final CommentRepository commentRepository;

    private final CommentLikeRepository commentLikeRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    //  댓글 작성
    @Transactional
    public CommentResponseDto create(
            CommentRequestDto commentRequestDto,
            MemberDto memberDto) {

        Member member = memberRepository.findById(memberDto.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Content content = contentRepository.findById(commentRequestDto.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Comment parent = null;

        if (commentRequestDto.getParentId() != null) {
            parent = commentRepository.findById(commentRequestDto.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .text(commentRequestDto.getText())
                .author(member)
                .content(content)
                .parent(parent)
                .build();

        CommentResponseDto commentResponseDto = CommentResponseDto.from(commentRepository.save(comment));

        // 좋아요 성능을 위해 댓글 번호 Redis에 등록
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        redisTemplate.opsForSet().add(redisValidComments, commentResponseDto.getId());

        return commentResponseDto;

    }

    @Transactional
    public CommentResponseDto update(
            Long id,
            CommentRequestDto commentRequestDto,
            MemberDto memberDto) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 등록자가 댓글의 주인인가?
        if (!comment.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);
        }

        comment.update(commentRequestDto.getText());

        return CommentResponseDto.from(comment);
    }

    @Transactional
    public void delete (
            Long id,
            MemberDto memberDto) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH));

        // 댓글 등록자가 댓글의 주인인가?
        if (!comment.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);
        }

        commentRepository.delete(comment);

        // 댓글 삭제시 Redis 캐시에서 글번호 삭제 (없는 댓글에 좋아요를 방지하기 위해서)
        String redisValidComment = PostRedisKey.VALID_COMMENTS.makeKey();
        redisTemplate.opsForSet().remove(redisValidComment, comment.getId());

    }

    public Long addLike(Long commentId, MemberDto memberDto) {

        // 댓글 조회
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        Boolean isValidComment = redisTemplate.opsForSet().isMember(redisValidComments, commentId);

        // Look-Aside 패턴 적용
        if (Boolean.FALSE.equals(isValidComment)) {

             boolean isComment = commentRepository.existsById(commentId);

             if (!isComment) throw  new BusinessException(ErrorCode.COMMENT_NOT_FOUND);

             redisTemplate.opsForSet().add(redisValidComments, commentId);
        }

        // 회원 조회
        Member member = memberRepository.getReferenceById(memberDto.getId());

        // Redis Set 사용
        String redisLikeCommentUsersSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        
        // Redis Set에 유저 ID 추가 시도
        // add() 결과 값 : 1 = 새로 추가됨(성공), 0 = 이미 있음(중복)
        Long isAdded = redisTemplate.opsForSet().add(redisLikeCommentUsersSet, member.getId());

        if (isAdded != null && isAdded == 0) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        String redisLikeCommentUsersQueue = PostRedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);

        // Redis 카운트 증가 (기본 로직 유지) 및 유저 추가
        String countKey = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        Long currentCount =  redisTemplate.opsForValue().increment(countKey);
        redisTemplate.opsForList().rightPush(redisLikeCommentUsersQueue, member.getId());

        // 커멘트 좋아요 카운트 더티 체킹
        redisTemplate.opsForSet().add(PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey(), commentId);

        return currentCount;

    }

    public void removeLike(Long commentId, MemberDto memberDto) {

        //  댓글과 회원 조회
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        Boolean isValidComment = redisTemplate.opsForSet().isMember(redisValidComments, commentId);

        // Look-Aside 구조
        if (Boolean.FALSE.equals(isValidComment)) {
            boolean isComment = commentRepository.existsById(commentId);
            if (!isComment) throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            // 롤백되면 바로 캐시 히트될 수 있게 저장
            redisTemplate.opsForSet().add(redisValidComments, commentId);
        }

        Member member = memberRepository.getReferenceById(memberDto.getId());

        //  Redis Set 사용
        String redisCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);

        //  remove() 결과 값 : 1 = 삭제, 0 = 없음
        Long isRemoved = redisTemplate.opsForSet().remove(redisCommentUserSet, member.getId());

        if (isRemoved != null && isRemoved == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }

        // 비동기 DB 삭제를 위한 삭제 대기열(Remove Queue) 추가
        String redisLikeCommentUsersRemoveQueue = PostRedisKey.LIKE_COMMENT_USERS_REMOVE_QUEUE.makeKey(commentId);
        redisTemplate.opsForList().rightPush(redisLikeCommentUsersRemoveQueue, member.getId());

        //  Redis 카운트 감소 (기본 로직 유지)
        String redisCountLikeKey = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        redisTemplate.opsForValue().decrement(redisCountLikeKey);

        //  게시글 좋아요 더티 체킹
        redisTemplate.opsForSet().add(PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey(), commentId);

    }

}
