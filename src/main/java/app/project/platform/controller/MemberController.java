package app.project.platform.controller;

import app.project.platform.domain.type.ApiResponse;
import app.project.platform.dto.LoginRequestDto;
import app.project.platform.dto.MemberDto;
import app.project.platform.dto.SignupRequestDTO;
import app.project.platform.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody @Valid LoginRequestDto loginRequestDto,
                                                     HttpServletRequest httpRequest // 세선을 위해 필요!
                                                     ) {

        MemberDto memberDto = memberService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        // 2. 세션 생성 (로그인 유지)
        HttpSession session = httpRequest.getSession();
        session.setAttribute("LOGIN_MEMBER", memberDto);
        session.setMaxInactiveInterval(60 * 30);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("로그인 성공"));

    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@RequestBody @Valid SignupRequestDTO signupRequestDTO) {

        Long memberId = memberService.signup(signupRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberId));

    }

}
