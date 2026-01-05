package app.project.platform.controller;

import app.project.platform.dto.MemberDTO;
import app.project.platform.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String login(MemberDTO memberDTO) {
        return "member/login_form";
    }

    @GetMapping("/signup")
    public String signupForm(MemberDTO memberDTO) {
        return "member/signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid MemberDTO memberDTO, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "member/signup_form";
        }

        try {
            memberService.join(memberDTO);
        } catch (IllegalStateException e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "member/signup_form";
        }

        return "redirect:/"; // 성공 시 메인으로

    }

}
