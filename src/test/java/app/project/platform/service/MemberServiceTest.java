package app.project.platform.service;

import app.project.platform.domain.AuthRedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import app.project.platform.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

// 1. Mockito를 쓰겠다고 선언
@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    // 2, 가짜(Mock) 객체 생성 : 실제 DB에 연결 안함
    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    JwtUtil jwtUtil;

    // 3. 가짜를 주입받을 진짜 객체 생성 : 가짜 rep가 여기로 쏙 들어감(DI)
    @InjectMocks
    MemberService memberService;

    @Test
    @DisplayName("회원가입 성공")
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
    @DisplayName("회원가입 실패 이메일중복")
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

    @Test
    @DisplayName("로그인 성공")
    void 로그인_성공() {

        // given
        LoginRequestDto loginRequestDto = LoginRequestDto.builder()
                .email("test@test.co.kr")
                .password("test")
                .build();

        Member member = Member.builder()
                        .email(loginRequestDto.getEmail())
                        .password("encoded_test_password")
                        .role(Role.USER)
                        .build();

        MemberDto memberDto = MemberDto.from(member);

        String accessToken = "testAccessToken";
        String refreshToken = "testRefreshToken";
        long refreshExpiration = 100000;

        given(memberRepository.findByEmail(loginRequestDto.getEmail())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(any(), any())).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(jwtUtil.createAccessToken(any())).willReturn(accessToken);
        given(jwtUtil.createRefreshToken(any())).willReturn(refreshToken);
        given(jwtUtil.getRefreshExpiration()).willReturn(refreshExpiration);

        // when
        TokenDto tokenDto = memberService.loginWithJwt(loginRequestDto);

        // then
        assertThat(tokenDto.getAccessToken()).isEqualTo(accessToken);
        assertThat(tokenDto.getRefreshToken()).isEqualTo(refreshToken);

        String redisRefreshToken = AuthRedisKey.REFRESH_TOKEN.makeRefreshToken(member.getEmail());

        verify(memberRepository, times(1)).findByEmail(eq(loginRequestDto.getEmail()));
        verify(passwordEncoder, times(1)).matches(eq(loginRequestDto.getPassword()), eq(member.getPassword()));
        verify(valueOperations, times(1)).set(eq(redisRefreshToken), eq(refreshToken), eq(refreshExpiration), eq(AuthRedisKey.REFRESH_TOKEN.getTimeUnit()));

    }

    @Test
    @DisplayName("리프레시 토큰 발급 성공")
    void 리프레시_토큰_재발급_성공() {

        //  given
        String token = "refreshToken";

        String email = "test@email.com";
        String role = Role.USER.getName();

        String redisRefreshToken = AuthRedisKey.REFRESH_TOKEN.makeRefreshToken(email);

        String newRefreshToken = "newRefreshToken";
        String accessToken = "accessToken";
        long refreshExpiration = 100000;

        given(jwtUtil.getEmailFromRefreshToken(eq(token))).willReturn(email);
        given(jwtUtil.getRoleFromRefreshToken(eq(token))).willReturn(role);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(eq(redisRefreshToken))).willReturn(token);
        given(jwtUtil.createRefreshToken(any())).willReturn(newRefreshToken);
        given(jwtUtil.createAccessToken(any())).willReturn(accessToken);
        given(jwtUtil.getRefreshExpiration()).willReturn(refreshExpiration);

        //   when
        TokenDto tokenDto = memberService.reissueWithJwt(token);

        assertThat(tokenDto.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(tokenDto.getAccessToken()).isEqualTo(accessToken);

        verify(jwtUtil, times(1)).validateRefreshToken(eq(token));
        verify(jwtUtil, times(1)).getEmailFromRefreshToken(eq(token));
        verify(jwtUtil, times(1)).getRoleFromRefreshToken(eq(token));
        verify(valueOperations, times(1)).get(eq(redisRefreshToken));
        verify(jwtUtil, times(1)).createRefreshToken(any());
        verify(jwtUtil, times(1)).createAccessToken(any());
        verify(valueOperations, times(1)).set(eq(redisRefreshToken), eq(newRefreshToken), eq(refreshExpiration), eq(AuthRedisKey.REFRESH_TOKEN.getTimeUnit()));

    }

    @Test
    @DisplayName("리프레시 토큰 발급 실패 만료")
    void 리프레시_토큰_재발급_실패_만료() {

        //  given
        String token = "refreshToken";

        willThrow(new ExpiredJwtException(null, null, "토큰만료됨"))
                .given(jwtUtil).validateRefreshToken(eq(token));

        //  when && then
        assertThatThrownBy(() -> memberService.reissueWithJwt(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPIRED_REFRESH_TOKEN);

        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("리프레시 토큰 발급 실패 불일치")
    void 리프레시_토큰_재발급_실패_불일치() {

        //  given
        String token = "missToken";

        String savedToken = "refreshToken";

        String email = "test@email.com";
        String role = Role.USER.getName();

        String redisRefreshToken = AuthRedisKey.REFRESH_TOKEN.makeRefreshToken(email);

        given(jwtUtil.getEmailFromRefreshToken(eq(token))).willReturn(email);
        given(jwtUtil.getRoleFromRefreshToken(eq(token))).willReturn(role);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(eq(redisRefreshToken))).willReturn(savedToken);

        //   when
        assertThatThrownBy(() -> memberService.reissueWithJwt(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_MISMATCH);

        verify(jwtUtil, times(1)).validateRefreshToken(eq(token));
        verify(jwtUtil, times(1)).getEmailFromRefreshToken(eq(token));
        verify(jwtUtil, times(1)).getRoleFromRefreshToken(eq(token));
        verify(valueOperations, times(1)).get(eq(redisRefreshToken));
        verify(redisTemplate, times(1)).delete(eq(redisRefreshToken));

    }

}
