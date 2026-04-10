package app.project.platform.controller;

import app.project.platform.domain.Word;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.domain.type.GrantType;
import app.project.platform.exception.BusinessException;
import app.project.platform.resolver.LoginUserArgumentResolver;
import app.project.platform.service.MemberService;
import app.project.platform.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    LoginUserArgumentResolver loginUserArgumentResolver;

    final String REQUEST_MAPPING = "/api/v1/member";

    @Test
    @DisplayName("회원가입 요청 성공")
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
    @DisplayName("로그인 요청 성공")
    void 로그인_요청_성공() throws Exception {

        LoginRequestDto requestDto = LoginRequestDto.builder()
                .email("test@email.com")
                .password("password")
                .build();

        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        TokenDto tokenDto = TokenDto.builder()
                .refreshToken(accessToken)
                .accessToken(refreshToken)
                .build();

        given(memberService.loginWithJwt(any())).willReturn(tokenDto);

        mockMvc.perform(post(REQUEST_MAPPING + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, tokenDto.getGrantType() + " " + tokenDto.getAccessToken()))
            .andExpect(cookie().maxAge(Word.RefreshToken.getWord(), 7 * 24 * 60 * 60))
//            .andExpect(cookie().secure(Word.RefreshToken.getWord(), true))
            .andExpect(cookie().path(Word.RefreshToken.getWord(), "/"))
            .andExpect(cookie().sameSite(Word.RefreshToken.getWord(), Word.Strict.getWord()))
            .andExpect(cookie().httpOnly(Word.RefreshToken.getWord(), true))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.accessToken").value(tokenDto.getAccessToken()))
            .andExpect(jsonPath("$.data.refreshToken").value(tokenDto.getRefreshToken()));
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 성공")
    void 리프레시_토큰_재발급_성공() throws Exception {

        // given
        String token = "token";

        String accessToken = "accessTokenValue";
        String refreshToken = "refreshTokenValue";

        TokenDto tokenDto = TokenDto.builder()
                .grantType(GrantType.Bearer.getType())
                .refreshToken(accessToken)
                .accessToken(refreshToken)
                .build();

        Cookie refresh = new Cookie(Word.RefreshToken.getWord(), token);
        refresh.setHttpOnly(true);

        given(memberService.reissueWithJwt(eq(token))).willReturn(tokenDto);

        //  when & then
        mockMvc.perform(post(REQUEST_MAPPING + "/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value(tokenDto.getAccessToken()))
                .andExpect(jsonPath("$.data.refreshToken").value(tokenDto.getRefreshToken()));

    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 만료")
    void 리프레시_토큰_재발급_실패_만료() throws Exception {

        // given
        String token = "token";

        Cookie refresh = new Cookie(Word.RefreshToken.getWord(), token);
        refresh.setHttpOnly(true);
        willThrow(new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN))
                .given(memberService).reissueWithJwt(token);

        //  when & then
        mockMvc.perform(post(REQUEST_MAPPING + "/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.EXPIRED_REFRESH_TOKEN.getMessage()));

    }

    @Test
    @DisplayName("리프레시 토큰 재발급 실패 불일치")
    void 리프레시_토큰_재발급_실패_불일치() throws Exception {

        // given
        String token = "token";

        Cookie refresh = new Cookie(Word.RefreshToken.getWord(), token);
        refresh.setHttpOnly(true);
        willThrow(new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH))
                .given(memberService).reissueWithJwt(token);

        //  when & then
        mockMvc.perform(post(REQUEST_MAPPING + "/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_MISMATCH.getMessage()));

    }

}
