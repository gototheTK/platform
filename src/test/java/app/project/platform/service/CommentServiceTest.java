package app.project.platform.service;

import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
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

        Member member = Member.builder()
                    .email(memberDto.getEmail())
                    .password("test")
                    .nickname("test")
                    .role(Role.USER)
                    .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        Content content = Content.builder()
                    .title("test")
                    .description("description")
                    .category(ContentCategory.CARTOON)
                    .author(member)
                    .build();

        ReflectionTestUtils.setField(content, "id", 1L);

        Comment expectedSavedComment = Comment.builder()
                .author(member)
                .text(commentRequestDto.getText())
                .content(content)
                .build();

        ReflectionTestUtils.setField(expectedSavedComment, "id", 1L);

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

        Member member = Member.builder()
                .email(memberDto.getEmail())
                .password("password")
                .nickname(memberDto.getNickname())
                .role(Role.USER)
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        Content content = Content.builder()
                .title("test_title")
                .description("test_description")
                .author(member)
                .category(ContentCategory.CARTOON)
                .build();

        ReflectionTestUtils.setField(content, "id", 1L);

        Comment parent = Comment.builder()
                .parent(null)
                .text("test_parent")
                .content(content)
                .author(null)
                .build();
        ReflectionTestUtils.setField(parent, "id", commentRequestDto.getParentId());

        Comment comment = Comment.builder()
                .parent(parent)
                .text(commentRequestDto.getText())
                .content(content)
                .author(member)
                .build();

        // given
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));
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

}
