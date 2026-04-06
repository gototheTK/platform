package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.Word;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
