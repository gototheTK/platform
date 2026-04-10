package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.Word;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.exception.BusinessException;
import app.project.platform.service.MemberService;
import app.project.platform.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@RequestBody @Valid SignupRequestDto signupRequestDto) {

        Long memberId = memberService.signup(signupRequestDto);

        return ResponseEntity
                .ok(ApiResponse.success(memberId));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenDto>> login(
            @RequestBody @Valid LoginRequestDto loginRequestDto,
            HttpServletResponse response
            ) {

        /**
        MemberDto memberDto = memberService.login(loginRequestDto);

        HttpSession session =httpServletRequest.getSession();
        session.setAttribute("LOGIN_MEMBER", memberDto);
        session.setMaxInactiveInterval(60 * 30);
         **/

        TokenDto tokenDto = memberService.loginWithJwt(loginRequestDto);

        // Refresh Token을 위한 HttpOnly 쿠키 생성
        ResponseCookie refreshTokenCookie = ResponseCookie.from(Word.RefreshToken.getWord(), tokenDto.getRefreshToken())
                .maxAge(7 * 24 * 60 * 60)
                .secure(false) // 개발중에는 Http를 사용함, Https를 사용안함
                .path("/")  // 모든 경로에서 쿠키 전송
                .sameSite(Word.Strict.getWord())    //  CSRF 방어를 위해 같은 도메인에서만 전송
                .httpOnly(true)
                .build();

        //  생성한 쿠키를 HTTP 응답 헤더(Set-Cookie)에 추가
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        //  엑세스 토큰은 기존처럼 Authorization 헤더와 바디에 담아서 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, tokenDto.getGrantType() + " " + tokenDto.getAccessToken())
                .body(ApiResponse.success(tokenDto));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenDto>> reissue (
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // 리프레시 토큰이 요청이 있을 경우 발급
        String refreshToken  = Objects.requireNonNull(Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.isHttpOnly() && cookie.getName().equals(Word.RefreshToken.getWord()))
                .findFirst().orElse(null)).getValue();

        TokenDto reissued = memberService.reissueWithJwt(refreshToken);

        // Refresh Token을 위한 HttpOnly 쿠키 생성
        ResponseCookie responseCookie = ResponseCookie.from(Word.RefreshToken.getWord(), reissued.getRefreshToken())
                .maxAge(7 * 24 * 60 * 60)
                .secure(false)  // 개발중에 Http를 사용함, Https를 사용안함
                .path("/")  // 모든 경로에서 쿠키 전송
                .sameSite(Word.Strict.getWord())    // CSRF 방어를 위해 같은 도메인에서만 전송
                .httpOnly(true)
                .build();

        //  생성된 쿠키를 HTTP 응답 헤더(Set-Cookie) 에추가
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, reissued.getGrantType() + " " + reissued.getAccessToken())
                .body(ApiResponse.success(reissued));
    }

}
