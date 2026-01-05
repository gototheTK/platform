package app.project.platform.service;

import app.project.platform.dto.MemberDTO;
import app.project.platform.entity.Member;
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
    public void join(MemberDTO memberDTO) {

        boolean isPresent = memberRepository.findByUsername(memberDTO.getUsername()).isPresent();

        if (isPresent) throw new IllegalArgumentException("{member.error.duplicate}");

        Member member = Member.builder()
                .username(memberDTO.getUsername())
                .password(passwordEncoder.encode(memberDTO.getPassword()))
                .email(memberDTO.getEmail())
                .build();

        memberRepository.save(member);

    }

}
