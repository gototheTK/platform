package app.project.platform.service;

import app.project.platform.domain.RedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.entity.Comment;
import app.project.platform.entity.CommentLike;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ContentRepository contentRepository;

    private final MemberRepository memberRepository;

    private final CommentRepository commentRepository;

    private final CommentLikeRepository commentLikeRepository;

    private final RedisTemplate<String, Object> redisTemplate;

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

        return CommentResponseDto.from(commentRepository.save(comment));

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
    }

    @Transactional
    public Long addLike(Long commentId, MemberDto memberDto) {

        // Redis 패턴
        String LIKE_COMMENT_USERS = RedisKey.LIKE_COMMENT_USERS.getPrefix();
        String LIKE_COMMENT_COUNT = RedisKey.LIKE_COMMENT_COUNT.getPrefix();
        String LIKE_UPDATED_COMMENTS = RedisKey.LIKE_UPDATED_COMMENTS.getPrefix();

        // 댓글과 회원 조회
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // Redis Set 사용
        String userLikeKey = LIKE_COMMENT_USERS + commentId;

        // Redis Set에 유저 ID 추가 시도
        // add() 결과 값 : 1 = 새로 추가됨(성공), 0 = 이미 있음(중복)
        Long isAdded = redisTemplate.opsForSet().add(userLikeKey, member.getId());

        if (isAdded != null && isAdded == 0) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        // DB 저장 (영속성 유지를 위해 DB에도 저장을 한다)
        CommentLike commentLike = CommentLike.builder()
                .comment(comment)
                .member(member)
                .build();
        CommentLike savedCommentLike = commentLikeRepository.save(commentLike);

        // Redis 카운트 증가 (기본 로직 유지)
        String countKey = LIKE_COMMENT_COUNT + commentId;
        redisTemplate.opsForValue().increment(countKey);

        // 커멘트 좋아요 카운트 더티 체킹
        redisTemplate.opsForSet().add(LIKE_UPDATED_COMMENTS, commentId);

        return savedCommentLike.getId();

    }

    @Transactional
    public void removeLike(Long commentId, MemberDto memberDto) {

        // Redis 패턴
        String LIKE_COMMENT_USERS = RedisKey.LIKE_COMMENT_USERS.getPrefix();
        String LIKE_COMMENT_COUNT = RedisKey.LIKE_COMMENT_COUNT.getPrefix();
        String LIKE_UPDATED_COMMENTS = RedisKey.LIKE_UPDATED_COMMENTS.getPrefix();

        //  댓글과 회원 조회
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        //  Redis Set 사용
        String userLikeKey = LIKE_COMMENT_USERS + commentId;

        //  Redis Set에 유저 Id 추가 시도
        //  remove() 결과 값 : 1 = 삭제, 0 = 없음
        Long isRemoved = redisTemplate.opsForSet().remove(userLikeKey, member.getId());

        if (isRemoved != null && isRemoved == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }

        commentLikeRepository.deleteByCommentAndMember(comment, member);

        //  Redis 카운트 감소 (기본 로직 유지)
        String countLikeKey = LIKE_COMMENT_COUNT + commentId;
        redisTemplate.opsForValue().decrement(countLikeKey);

        //  게시글 좋아요 더티 체킹
        redisTemplate.opsForSet().add(LIKE_UPDATED_COMMENTS, commentId);

    }

}
