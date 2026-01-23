package app.project.platform.controller;

import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.service.ContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ContentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ContentService contentService;

    final String REQUEST_MAPPING = "/api/v1/content";

    @Test
    void 글_생성 () throws Exception {

        ContentUpdateRequestDto contentRequestDto = ContentUpdateRequestDto.builder()
                .id(1L)
                .title("test")
                .description("description")
                .category(ContentCategory.CARTOON.getName())
                .build();

        MemberDto memberDto = MemberDto.builder().id(1L).build();

        Long contentId = 1L;

        // JSON DTO를 "파일"처럼 만듭니다.
        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsString(contentRequestDto).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile files = new MockMultipartFile(
                "files",
                "",
                "application/octet-stream",
                new byte[0]
        );

        //  given
        given(contentService.create(any(), any(), any())).willReturn(contentId);

        //  when & then
        mockMvc.perform(multipart(REQUEST_MAPPING)
                .file(requestPart)
                .file(files)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .sessionAttr("LOGIN_MEMBER", memberDto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(contentId));

    }
    
    @Test
    void 글_생성_실패_제목누락() throws Exception {

        ContentCreateRequestDto requestDto = ContentCreateRequestDto.builder()
                .title("")
                .category(ContentCategory.CARTOON.getName())
                .description("내용")
                .build();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile files = new MockMultipartFile(
                "files",
                "",
                "application/octet-stream",
                new byte[0]
        );

        mockMvc.perform(multipart(REQUEST_MAPPING)
                .file(requestPart)
                .file(files)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept("application/json")
                .sessionAttr("LOGIN_MEMBER", MemberDto.builder().id(1L).build()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"));
        
    }

    @Test
    void 글_수정() throws Exception {

        Long contentId = 1L;

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        ContentUpdateRequestDto requestDto = ContentUpdateRequestDto.builder()
                .id(contentId)
                .title("test_title")
                .description("test_description")
                .category(ContentCategory.CARTOON.getName())
                .build();

        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile files = new MockMultipartFile(
                "files",
                "",
                "application/octet-stream",
                new byte[0]
        );

        ContentResponseDto responseDto = ContentResponseDto.builder()
                .id(contentId)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .nickname(memberDto.getNickname())
                .comments(null)
                .category(requestDto.getCategory())
                .build();

        //  given
        given(contentService.update(any(), any(), any(), any())).willReturn(responseDto);

        //  when & then
        mockMvc.perform(multipart(REQUEST_MAPPING+"/1")
                .file(request)
                .file(files)
                .with(req -> {req.setMethod("PUT"); return req;})
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept("application/json")
                .sessionAttr("LOGIN_MEMBER", memberDto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(requestDto.getId()))
            .andExpect(jsonPath("$.data.title").value(requestDto.getTitle()))
            .andExpect(jsonPath("$.data.description").value(requestDto.getDescription()))
            .andExpect(jsonPath("$.data.category").value(requestDto.getCategory()));

    }

    @Test
    void 글_수정_실패_ID누락() throws Exception {

        ContentUpdateRequestDto requestDto = ContentUpdateRequestDto.builder()
                .title("test_title")
                .description("test_description")
                .build();

        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                "application/json",
                objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile files = new MockMultipartFile(
                "files",
                "",
                "application/octet-stream",
                new byte[0]
        );

        // when & then
        mockMvc.perform(multipart(REQUEST_MAPPING+"/1")
                .file(request)
                .file(files)
                .with(req -> {req.setMethod("PUT"); return req;})
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .sessionAttr("LOGIN_MEMBER", MemberDto.builder().id(1L).build()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("fail"));

    }

}
