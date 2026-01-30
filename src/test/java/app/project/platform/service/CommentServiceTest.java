package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.*;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentLikeRepository;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

        //given
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));
        given(contentRepository.findById(commentRequestDto.getContentId())).willReturn(Optional.of(content));

        given(commentRepository.save(any())).willReturn(expectedSavedComment);

        //when
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDto);

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(contentRepository, times(1)).findById(commentRequestDto.getContentId());
        verify(commentRepository, times(1)).save(captor.capture());

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

        Comment parent = createComment(1L, content, null, stranger, "test_parent");

        Comment comment = createComment(2L, content, parent, writer, commentRequestDto.getText());

        // given
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(writer));
        given(contentRepository.findById(commentRequestDto.getContentId())).willReturn(Optional.of(content));
        given(commentRepository.findById(commentRequestDto.getParentId())).willReturn(Optional.of(parent));

        given(commentRepository.save(any())).willReturn(comment);

        // when
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDto);

        // then
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(contentRepository, times(1)).findById(commentRequestDto.getContentId());
        verify(commentRepository, times(1)).findById(commentRequestDto.getParentId());

        verify(commentRepository, times(1)).save(captor.capture());

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
    @DisplayName("댓글 좋아요 성공")
    void 댓글_좋아요_성공 () {

        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        Comment comment = Comment.builder().build();
        ReflectionTestUtils.setField(comment, "id", 1L);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        CommentLike commentLike = CommentLike.builder().build();
        ReflectionTestUtils.setField(commentLike, "id", 1L);

        // given
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.add(any(), any())).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(commentLikeRepository.save(any())).willAnswer(invocationOnMock -> {
            CommentLike arg = invocationOnMock.getArgument(0);
            ReflectionTestUtils.setField(arg, "id", 1L);
            return arg;
        });

        // when
        Long commentLikeId = commentService.addLike(commentId, memberDto);

        // then
        assertThat(commentLikeId).isEqualTo(commentLike.getId());

        ArgumentCaptor<CommentLike> captor = ArgumentCaptor.forClass(CommentLike.class);

        verify(commentRepository, times(1)).findById(commentId);
        verify(memberRepository, times(1)).findById(memberDto.getId());
        String expectedUserKey = "like:comment:users:"+commentId;
        verify(setOperations, times(1)).add(eq(expectedUserKey), eq(member.getId()));
        verify(commentLikeRepository, times(1)).save(captor.capture());
        String expectedCountKey = "like:comment:count:"+commentId;
        verify(valueOperations, times(1)).increment(eq(expectedCountKey));

        CommentLike capturedCommentLike = captor.getValue();

        assertThat(capturedCommentLike.getComment()).isEqualTo(comment);
        assertThat(capturedCommentLike.getMember()).isEqualTo(member);

    }

    @Test
    @DisplayName("댓글 좋아요 실패 중복")
    void 댓글_좋아요_실패_중복 () {

        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        //  댓글과 회원 조회
        Comment comment = Comment.builder().build();
        ReflectionTestUtils.setField(comment, "id", commentId);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        //  given
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.add(any(), any())).willReturn(0L);

        //  when & then
        assertThatThrownBy(() -> commentService.addLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_LIKED);

        verify(commentLikeRepository, times(0)).save(any());
        verify(valueOperations, times(0)).increment(any());

    }

    @Test
    @DisplayName("댓글 좋아요 취소 성공")
    void 댓글_좋아요_취소_성공 () {

        //  입력 인자
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        //  댓글과 회원 조회
        Comment comment = Comment.builder().build();
        ReflectionTestUtils.setField(comment, "id", commentId);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        //  given
        // JPA 메서드
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        // Redis 메서드
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.remove(any(), any())).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        //  when
        commentService.removeLike(commentId, memberDto);

        ArgumentCaptor<Comment> captorComment = ArgumentCaptor.forClass(Comment.class);
        ArgumentCaptor<Member> captorMember = ArgumentCaptor.forClass(Member.class);

        //  then
        verify(commentRepository, times(1)).findById(commentId);
        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(commentLikeRepository, times(1)).deleteByCommentAndMember(captorComment.capture(), captorMember.capture());
        verify(setOperations, times(1)).remove(any(), any());
        verify(valueOperations, times(1)).decrement(any());

        Comment capturedComment = captorComment.getValue();
        Member capturedMember = captorMember.getValue();

        assertThat(capturedComment).isEqualTo(comment);
        assertThat(capturedMember).isEqualTo(member);

    }

    @Test
    @DisplayName("댓글 좋아요 취소 실패 미존재")
    void 댓글_좋아요_취소_실패_미존재 () {

        //  파라미터 인자들
        Long commentId = 1L;
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        //  댓글과 회원 조회
        Comment comment = Comment.builder().build();
        ReflectionTestUtils.setField(comment, "id", 1L);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", 1L);

        //  given
        // JPA 메서드
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        // Redis 메서드
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.remove(any(), any())).willReturn(0L);

        assertThatThrownBy(()->commentService.removeLike(commentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIKE_NOT_FOUND);

        verify(commentRepository, times(1)).findById(commentId);
        verify(memberRepository, times(1)).findById(memberDto.getId());
        String expectedKey = "like:comment:users:" + commentId;
        verify(setOperations, times(1)).remove(eq(expectedKey), eq(member.getId()));
        verify(commentLikeRepository, times(0)).deleteByCommentAndMember(any(), any());
        verify(valueOperations, times(0)).decrement(any());

    }

}
