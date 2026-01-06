package app.project.platform.service;

import app.project.platform.domain.type.ErrorCode;
import app.project.platform.dto.MemberDto;
import app.project.platform.dto.SignupRequestDTO;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    @Transactional
    public Long signup(SignupRequestDTO signupRequestDTO) {

        Member member = Member.builder()
                        .username(signupRequestDTO.getUsername())
                        .password(passwordEncoder.encode(signupRequestDTO.getPassword()))
                        .email(signupRequestDTO.getEmail())
                        .build();

        return memberRepository.save(member).getId();

    }

    @Transactional(readOnly = true)
    public MemberDto login(String email, String password) {

        return memberRepository
                .findByEmailAndPassword(email, passwordEncoder.encode(password))
                .orElseThrow((()->new BusinessException(ErrorCode.UNAUTHORIZED)));

    }

}
