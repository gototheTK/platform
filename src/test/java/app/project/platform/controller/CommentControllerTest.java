package app.project.platform.controller;

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
import app.project.platform.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CommentService commentService;

    @Test
    void 댓글_작성 () throws Exception {

        CommentRequestDto requestDto = CommentRequestDto.builder()
                .contentId(1L)
                .text("test_text")
                .parentId(null)
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        Member member = Member.builder()
                .email(memberDto.getEmail())
                .password("test_password")
                .role(Role.USER)
                .nickname("test")
                .build();

        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        Content content = Content.builder()
                .author(member)
                .title("test_title")
                .description("test_description")
                .category(ContentCategory.CARTOON)
                .build();

        Comment comment = Comment.builder()
                .author(member)
                .content(content)
                .text(requestDto.getText())
                .build();

        ReflectionTestUtils.setField(comment, "id", 1L);

        CommentResponseDto responseDto = CommentResponseDto.from(comment);

        given(commentService.create(any(), any())).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .sessionAttr("LOGIN_MEMBER", memberDto))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.author").value(memberDto.getNickname()))
            .andExpect(jsonPath("$.data.text").value(requestDto.getText()));

    }

    @Test
    void 댓글_수정 () throws Exception {

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .id(1L)
                .contentId(1L)
                .text("test_text")
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .nickname("test_nickname")
                .build();

        CommentResponseDto commentResponseDto = CommentResponseDto.builder()
                .id(commentRequestDto.getId())
                .author(memberDto.getNickname())
                .text(commentRequestDto.getText())
                .build();

        given(commentService.update(any(), any(), any())).willReturn(commentResponseDto);

        mockMvc.perform(put("/api/v1/comment" + "/1")
                .content(objectMapper.writeValueAsString(commentRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("LOGIN_MEMBER", memberDto))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.id").value(commentRequestDto.getId()))
            .andExpect(jsonPath("$.data.author").value(memberDto.getNickname()))
            .andExpect(jsonPath("$.data.text").value(commentRequestDto.getText()));

    }

    @Test
    void 댓글_수정_실패_회원불일치() throws Exception {

        Long commentId = 1L;
        Long contentId = 1L;
        String text = "test_text";

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .id(commentId)
                .contentId(contentId)
                .text(text)
                .build();

        //  gvien
        given(commentService.update(any(), any(), any())).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when
        mockMvc.perform(put("/api/v1/comment/" + commentId)
                .content(objectMapper.writeValueAsString(commentRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("LOGIN_MEMBER", MemberDto.builder().id(1L).build()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
            .andExpect(jsonPath("$.data").isEmpty())
            .andDo(print());

    }


}
