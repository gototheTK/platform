package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<MemberDto>> login(
            @RequestBody @Valid LoginRequestDto loginRequestDto,
            HttpServletRequest httpServletRequest
            ) {

        MemberDto memberDto = memberService.login(loginRequestDto);

        HttpSession session =httpServletRequest.getSession();
        session.setAttribute("LOGIN_MEMBER", memberDto);
        session.setMaxInactiveInterval(60 * 30);

        return ResponseEntity
                .ok(ApiResponse.success(memberDto));
    }

}
