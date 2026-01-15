package app.project.platform.service;


import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
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
    public Long signup(SignupRequestDto signupRequestDto) {

        // 이메일 중복 확인
        boolean isEmailDuplicate = memberRepository.findByEmail(signupRequestDto.getEmail()).isPresent();

        if (isEmailDuplicate) throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);

        // 닉네임 중복 확인
        boolean isNicknameDuplicate = memberRepository.findByNickname(signupRequestDto.getNickname()).isPresent();

        if (isNicknameDuplicate) throw new BusinessException(ErrorCode.NICKNAME_DUPLICATION);

        Member member = Member.builder()
                .email(signupRequestDto.getEmail())
                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                .nickname(signupRequestDto.getNickname())
                .role(Role.USER)
                .build();

        return memberRepository.save(member).getId();
    }

    // 조회 전용 트랜잭션에서는 JPA가 스냅샷을 만들지 않고, Dirty Checking(변경 감지)을 수행하지 않아 메모리와 성능이 최적화
    // 리플리카 DB(Slave) 부하 분산 효과도 있습니다.
    @Transactional(readOnly = true)
    public MemberDto login(LoginRequestDto loginRequestDto) {

        Member member = memberRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return MemberDto.from(member);

    }

}
