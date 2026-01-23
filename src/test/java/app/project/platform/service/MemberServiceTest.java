package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;

// 1. Mockito를 쓰겠다고 선언
@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    // 2, 가짜(Mock) 객체 생성 : 실제 DB에 연결 안함
    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    // 3. 가짜를 주입받을 진짜 객체 생성 : 가짜 rep가 여기로 쏙 들어감(DI)
    @InjectMocks
    MemberService memberService;

    @Test
    void 회원가입_성공() {
        // given (준비: 이런 상황이 주어졌을 때)
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .email("test@test.co.kr")
                .password("test")
                .nickname("test")
                .build();

        Member member = Member.builder()
                .email(signupRequestDto.getEmail())
                .password(signupRequestDto.getPassword())
                .nickname(signupRequestDto.getNickname())
                .role(Role.USER)
                .build();


        // "가짜 Repo야, 누가 'hello'를 찾으면 '없음(empty)'이라고 거짓말해줘!
        given(passwordEncoder.encode(any())).willReturn("encoded_test");
        given(memberRepository.save(any())).willReturn(member);

        // when (실행: 이 메서드를 실행하면)
        Long saveId = memberService.signup(signupRequestDto);

        verify(memberRepository, times(1)).save(any());

        assertThat(saveId).isEqualTo(member.getId());

    }

    @Test
    void 회원가입_실패_이메일중복() {

        // given
        SignupRequestDto requestDto = SignupRequestDto.builder()
                .email("test@test.co.kr")
                .password("test")
                .nickname("test")
                .build();

        Member member = Member.builder()
                        .email(requestDto.getEmail())
                        .password(requestDto.getPassword())
                        .nickname(requestDto.getNickname())
                        .build();

        given(memberRepository.findByEmail("test@test.co.kr"))
                .willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.signup(requestDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.EMAIL_DUPLICATION.getMessage());

    }

}
