package app.project.platform.service;

import app.project.platform.domain.PostRedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Comment;
import app.project.platform.entity.CommentLike;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentLikeRepository;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    ContentRepository contentRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    CommentLikeRepository commentLikeRepository;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ListOperations<String, Object> listOperations;

    @Mock
    SetOperations<String, Object> setOperations;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp () {}

    @Test
    void 댓글_작성() {

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .text("test!!!")
                .contentId(1L)
                .parentId(null)
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.co.kr")
                .role(Role.USER.getName())
                .nickname("test")
                .build();

        Member member = createMember(memberDto.getId(), memberDto.getNickname());

        Content content = createContent(1L, member);

        Comment expectedSavedComment = createComment(1L, content, null, member, commentRequestDto.getText());

        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();

        //given
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));
        given(contentRepository.findById(commentRequestDto.getContentId())).willReturn(Optional.of(content));
        given(commentRepository.save(any())).willReturn(expectedSavedComment);
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        //when
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDto);

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(contentRepository, times(1)).findById(commentRequestDto.getContentId());
        verify(commentRepository, times(1)).save(captor.capture());
        verify(setOperations, times(1)).add(eq(redisValidComments), eq(commentResponseDto.getId()));

        Comment capturedComment = captor.getValue();

        assertThat(capturedComment.getText()).isEqualTo(commentRequestDto.getText());
        assertThat(capturedComment.getContent().getId()).isEqualTo(content.getId());
        assertThat(capturedComment.getAuthor().getId()).isEqualTo(member.getId());
        assertThat(capturedComment.getParent()).isNull();

        assertThat(commentResponseDto.getText()).isEqualTo(commentRequestDto.getText());
        assertThat(commentResponseDto.getAuthor()).isEqualTo(memberDto.getNickname());

    }

    @Test
    void 대댓글_작성() {

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .parentId(100L)
                .text("comment_test")
                .contentId(1L)
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .build();

        Member writer = createMember(memberDto.getId(), memberDto.getNickname());

        Member stranger = createMember(2L, "stranger");

        Content content = createContent(1L, writer);

        Comment parent = createComment(100L, content, null, stranger, "test_parent");

        Comment comment = createComment(2L, content, parent, writer, commentRequestDto.getText());

        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();

        // given
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(writer));
        given(contentRepository.findById(commentRequestDto.getContentId())).willReturn(Optional.of(content));
        given(commentRepository.findById(commentRequestDto.getParentId())).willReturn(Optional.of(parent));

        given(commentRepository.save(any())).willReturn(comment);
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // when
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDto);

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(contentRepository, times(1)).findById(commentRequestDto.getContentId());
        verify(commentRepository, times(1)).findById(commentRequestDto.getParentId());

        verify(commentRepository, times(1)).save(captor.capture());
        verify(setOperations, times(1)).add(eq(redisValidComments), eq(commentResponseDto.getId()));

        Comment capturedComment = captor.getValue();

        assertThat(capturedComment.getText()).isEqualTo(commentRequestDto.getText());
        assertThat(capturedComment.getContent().getId()).isEqualTo(commentRequestDto.getContentId());
        assertThat(capturedComment.getParent().getId()).isEqualTo(commentRequestDto.getParentId());
        assertThat(capturedComment.getAuthor().getId()).isEqualTo(memberDto.getId());

        assertThat(commentResponseDto.getText()).isEqualTo(commentRequestDto.getText());
        assertThat(commentResponseDto.getAuthor()).isEqualTo(memberDto.getNickname());

    }

    @Test
    void 댓글_수정_실패_권한없음() {

        Long commentId = 1L;

        MemberDto memberDto = MemberDto.builder().id(1L).build();

        Member writer = createMember(1L, "writer");
        Member stranger = createMember(2L, "stranger");

        Content content = createContent(1L, writer);

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .text("test")
                .build();

        Comment comment = createComment(1L, content, null, stranger, stranger.getNickname() + "의 테스트");

        //  given
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        //  when & then
        assertPermissionDenied(() -> commentService.update(commentId, commentRequestDto, memberDto));
    }

    @Test
    void 댓글_삭제_실패_권한없음() {

        Long commentId = 1L;

        MemberDto memberDto = MemberDto.builder().id(1L).build();

        Member writer = createMember(1L, "writer");
        Member stranger = createMember(2L, "stranger");

        Content content = createContent(1L, writer);

        Comment comment = createComment(1L, content, null, stranger, stranger.getNickname() + "의 테스트");

        //  given
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        //  when & then
        assertPermissionDenied(() -> commentService.delete(commentId, memberDto));

    }

    private Member createMember(Long id, String nickname) {
        Member member = Member.builder()
                .email(nickname + "@test.com")
                .password("password")
                .nickname(nickname)
                .role(Role.USER)
                .build();

        ReflectionTestUtils.setField(member, "id", id);

        return member;
    }

    private Content createContent(Long id, Member author) {
        Content content = Content.builder()
                .author(author)
                .title(author.getNickname() + "의 게시글")
                .description("test_description")
                .category(ContentCategory.CARTOON)
                .build();

        ReflectionTestUtils.setField(content, "id", id);

        return content;

    }

    private Comment createComment(Long id, Content content, Comment parent, Member writer, String text) {
        Comment comment = Comment.builder()
                .author(writer)
                .content(content)
                .text(text)
                .parent(parent)
                .build();

        ReflectionTestUtils.setField(comment, "id", id);

        return comment;

    }

    private void assertPermissionDenied(ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.COMMENT_WRITER_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("댓글 좋아요 성공 댓글 캐시 히트")
    void 댓글_좋아요_성공_댓글_캐시_히트 () {

        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        CommentLike commentLike = CommentLike.builder().build();
        ReflectionTestUtils.setField(commentLike, "id", 1L);

        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisLikeCommentCount = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        String redisLikeCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserQueue = PostRedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);
        String redisLikeUpdatedComments = PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey();
        long expectedTotalCount = 15L;

        // given
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember(redisValidComments, commentId)).willReturn(true);
        given(memberRepository.getReferenceById(memberDto.getId())).willReturn(member);
        given(setOperations.add(eq(redisLikeCommentUserSet), eq(memberDto.getId()))).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        given(valueOperations.increment(redisLikeCommentCount)).willReturn(expectedTotalCount);

        // when
        Long totalCount = commentService.addLike(commentId, memberDto);

        // then
        assertThat(totalCount).isEqualTo(expectedTotalCount);

        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(memberDto.getId());
        verify(setOperations, times(1)).add(eq(redisLikeCommentUserSet), eq(member.getId()));
        verify(valueOperations, times(1)).increment(eq(redisLikeCommentCount));
        verify(listOperations, times(1)).rightPush(eq(redisLikeCommentUserQueue), eq(member.getId()));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedComments), eq(commentId));

    }

    @Test
    @DisplayName("댓글 좋아요 성공 댓글 캐시 미스 DB 조회")
    void 댓글_좋아요_성공_댓글_캐시_미스_DB_조회 () {

        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        CommentLike commentLike = CommentLike.builder().build();
        ReflectionTestUtils.setField(commentLike, "id", 1L);

        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisLikeCommentCount = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        String redisLikeCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserQueue = PostRedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);
        String redisLikeUpdatedComments = PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey();
        long expectedTotalCount = 15L;

        // given
        // Redis 리턴
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);


        given(setOperations.isMember(redisValidComments, commentId)).willReturn(false);
        given(commentRepository.existsById(commentId)).willReturn(true);
        given(setOperations.add(eq(redisValidComments), eq(commentId))).willReturn(1L);
        given(memberRepository.getReferenceById(memberDto.getId())).willReturn(member);
        given(setOperations.add(eq(redisLikeCommentUserSet), eq(memberDto.getId()))).willReturn(1L);
        given(valueOperations.increment(redisLikeCommentCount)).willReturn(expectedTotalCount);

        // when
        Long totalCount = commentService.addLike(commentId, memberDto);

        // then
        assertThat(totalCount).isEqualTo(expectedTotalCount);

        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(setOperations, times(1)).add(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(memberDto.getId());
        verify(setOperations, times(1)).add(eq(redisLikeCommentUserSet), eq(member.getId()));
        verify(valueOperations, times(1)).increment(eq(redisLikeCommentCount));
        verify(listOperations, times(1)).rightPush(eq(redisLikeCommentUserQueue), eq(member.getId()));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedComments), eq(commentId));

    }

    @Test
    @DisplayName("댓글 좋아요 실패 댓글 미존재")
    void 댓글_좋아요_실패_댓글_미존재 () {

        // 메서드 인자 초기화
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        // RedisKey 초기화
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();

        // given
        // Redis 처리 초기화
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        //  반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(false);
        given(commentRepository.existsById(commentId)).willReturn(false);

        //  when & then
        assertThatThrownBy(() -> commentService.addLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(commentRepository, times(1)).existsById(commentId);

    }

    @Test
    @DisplayName("댓글 좋아요 실패 중복")
    void 댓글_좋아요_실패_중복 () {

        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        // 회원 조회
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        // Redis key
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisLikeCommentUsersSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);

        //  given
        //  Redis 처리
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        
        // 반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(true);
        given(memberRepository.getReferenceById(memberDto.getId())).willReturn(member);
        given(setOperations.add(eq(redisLikeCommentUsersSet), eq(member.getId()))).willReturn(0L);

        //  when & then
        assertThatThrownBy(() -> commentService.addLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_LIKED);

        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(memberDto.getId());
        verify(setOperations, times(1)).add(eq(redisLikeCommentUsersSet), eq(member.getId()));

    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공 캐시 히트")
    void 댓글_좋아요_취소_성공_캐시_히트 () {

        //  입력 인자 및 조회 데이터
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        // Redis Key
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserRemoveQueue = PostRedisKey.LIKE_COMMENT_USERS_REMOVE_QUEUE.makeKey(commentId);
        String redisCountLikeKey = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        String redisLikeUpdatedComments = PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey();

        //  given
        // Redis 처리
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // 반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(true);
        given(memberRepository.getReferenceById(eq(memberDto.getId()))).willReturn(member);
        given(setOperations.remove(eq(redisCommentUserSet), eq(member.getId()))).willReturn(1L);

        //  when
        commentService.removeLike(commentId, memberDto);

        //  then
        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(eq(memberDto.getId()));
        verify(setOperations, times(1)).remove(eq(redisCommentUserSet), eq(member.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeCommentUserRemoveQueue), eq(member.getId()));
        verify(valueOperations, times(1)).decrement(redisCountLikeKey);
        verify(setOperations, times(1)).add(redisLikeUpdatedComments, commentId);

    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공 캐시 미스 DB 조회")
    void 댓글_좋아요_취소_성공_캐시_미스_DB_조회 () {

        //  입력 인자 및 조회 데이터
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        // Redis Key
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserRemoveQueue = PostRedisKey.LIKE_COMMENT_USERS_REMOVE_QUEUE.makeKey(commentId);
        String redisCountLikeKey = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        String redisLikeUpdatedComments = PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey();

        //  given
        // Redis 처리
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // 반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(false);
        given(commentRepository.existsById(eq(commentId))).willReturn(true);
        given(memberRepository.getReferenceById(eq(memberDto.getId()))).willReturn(member);
        given(setOperations.remove(eq(redisCommentUserSet), eq(member.getId()))).willReturn(1L);

        //  when
        commentService.removeLike(commentId, memberDto);

        //  then
        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(commentRepository, times(1)).existsById(eq(commentId));
        verify(setOperations, times(1)).add(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(eq(memberDto.getId()));
        verify(setOperations, times(1)).remove(eq(redisCommentUserSet), eq(member.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeCommentUserRemoveQueue), eq(member.getId()));
        verify(valueOperations, times(1)).decrement(redisCountLikeKey);
        verify(setOperations, times(1)).add(redisLikeUpdatedComments, commentId);

    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 댓글 미존재")
    void 댓글_좋아요_취소_실패_댓글_미존재 () {

        //  입력 인자 및 조회 데이터
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        // Redis Key
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();

        //  given
        // Redis 처리
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(false);
        given(commentRepository.existsById(eq(commentId))).willReturn(false);

        assertThatThrownBy(() -> commentService.removeLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);

        // when & then
        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(commentRepository, times(1)).existsById(eq(commentId));

    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 좋아요 미존재")
    void 댓글_좋아요_취소_실패_좋아요_미존재 () {

        //  입력 인자 및 조회 데이터
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        // Redis Key
        String redisValidComments = PostRedisKey.VALID_COMMENTS.makeKey();
        String redisCommentUserSet = PostRedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);

        //  given
        // Redis 처리
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 반환 로직
        given(setOperations.isMember(eq(redisValidComments), eq(commentId))).willReturn(true);
        given(memberRepository.getReferenceById(eq(memberDto.getId()))).willReturn(member);
        given(setOperations.remove(eq(redisCommentUserSet), eq(member.getId()))).willReturn(0L);

        //  when
        assertThatThrownBy(()->commentService.removeLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e->((BusinessException)e).getErrorCode())
                .isEqualTo(ErrorCode.LIKE_NOT_FOUND);

        //  then
        verify(setOperations, times(1)).isMember(eq(redisValidComments), eq(commentId));
        verify(memberRepository, times(1)).getReferenceById(eq(memberDto.getId()));
        verify(setOperations, times(1)).remove(eq(redisCommentUserSet), eq(member.getId()));

    }

}
