package app.project.platform.controller;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.exception.BusinessException;
import app.project.platform.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    @DisplayName("댓글 작성 성공")
    void 댓글_작성_성공 () throws Exception {

        CommentRequestDto requestDto = CommentRequestDto.builder()
                .contentId(1L)
                .text("test_text")
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .nickname("test")
                .build();

        CommentResponseDto responseDto = CommentResponseDto.builder()
                .id(1L)
                .author("test")
                .text("test_text")
                .build();

        // given
        given(commentService.create(any(), any())).willReturn(responseDto);

        // when & then
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
    @DisplayName("댓글 수정 성공")
    void 댓글_수정_성공 () throws Exception {

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

        // given
        given(commentService.update(any(), any(), any())).willReturn(commentResponseDto);

        // when & then
        mockMvc.perform(put("/api/v1/comment/{id}", 1L)
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
    @DisplayName("댓글 수정 실패 회원 불일치")
    void 댓글_수정_실패_회원_불일치() throws Exception {

        Long commentId = 1L;
        Long contentId = 1L;
        String text = "test_text";

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .id(commentId)
                .contentId(contentId)
                .text(text)
                .build();

        // gvien
        given(commentService.update(any(), any(), any())).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        mockMvc.perform(put("/api/v1/comment/{id}", commentId)
                .content(objectMapper.writeValueAsString(commentRequestDto))
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("LOGIN_MEMBER", MemberDto.builder().id(1L).build()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
            .andExpect(jsonPath("$.data").isEmpty())
            .andDo(print());

    }


}
