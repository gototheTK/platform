package app.project.platform.controller;

import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.domain.type.Role;
import app.project.platform.repository.MemberRepository;
import app.project.platform.resolver.LoginUserArgumentResolver;
import app.project.platform.service.MemberService;
import app.project.platform.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MemberControllerTest {

    @Autowired
    MockMvc mockMvc; // 2. HTTP 요청을 흉내 내는 도구 (post, get 등)

    @MockitoBean
    MemberService memberService;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    JwtUtil jwtUtil;

    @MockitoBean
    MemberRepository memberRepository;

    @MockitoBean
    LoginUserArgumentResolver loginUserArgumentResolver;

    final String REQUEST_MAPPING = "/api/v1/member";

    @Test
    void 회원가입_요청_성공() throws Exception {

        SignupRequestDto requestDto = SignupRequestDto.builder()
                .email("test@test.co.kr")
                .password("test")
                .nickname("test")
                .build();

        given(memberService.signup(any())).willReturn(1L);

        mockMvc.perform(post("/api/v1/member/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    void 로그인_요청_성공() throws Exception {

        LoginRequestDto requestDto = LoginRequestDto.builder()
                .email("test@email.com")
                .password("password")
                .build();

        MemberDto memberDto = MemberDto.builder()
                    .id(1L)
                    .email(requestDto.getEmail())
                    .nickname("test")
                    .role(Role.USER.getName())
                    .build();

        String accessToken = jwtUtil.createRefreshToken(memberDto);
        String refreshToken = jwtUtil.createAccessToken(memberDto);

        TokenDto tokenDto = TokenDto.builder()
                .refreshToken(accessToken)
                .accessToken(refreshToken)
                .build();

        given(memberService.loginWithJwt(any())).willReturn(tokenDto);

        mockMvc.perform(post(REQUEST_MAPPING + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.accessToken").value(tokenDto.getAccessToken()))
            .andExpect(jsonPath("$.data.refreshToken").value(tokenDto.getRefreshToken()));
    }

}
