package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;

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

}
