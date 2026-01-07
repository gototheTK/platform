package app.project.platform.service;

import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    @Transactional
    public Long signup(SignupRequestDto signupDto) {

        if (memberRepository.findByEmail(signupDto.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }

        if (memberRepository.findByNickname(signupDto.getNickname()).isPresent()) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATION);
        }

        Member member = Member.builder()
                .email(signupDto.getEmail())
                .password(passwordEncoder.encode(signupDto.getPassword()))
                .nickname(signupDto.getNickname())
                .role(Role.USER)
                .build();

        MemberDto memberDto = MemberDto.from(memberRepository.save(member));

        return memberDto.getId();

    }

    @Transactional(readOnly = true)
    public MemberDto login(LoginRequestDto loginRequestDto) {

        Member member = memberRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword())) throw new BusinessException(ErrorCode.LOGIN_FAILED);

        return MemberDto.from(member);

    }

}
