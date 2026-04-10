package app.project.platform.service;


import app.project.platform.domain.AuthRedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.LoginRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.domain.type.GrantType;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import app.project.platform.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

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

    // 조회 전용 트랜잭션에서는 JPA가 스냅샷을 만들지 않고, Dirty Checking(변경 감지)을 수행하지 않아 메모리와 성능이 최적화
    // 리플리카 DB(Slave) 부하 분산 효과도 있습니다.
    @Transactional(readOnly = true)
    public TokenDto loginWithJwt(LoginRequestDto loginRequestDto) {

        Member member = memberRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtUtil.createAccessToken(MemberDto.from(member));
        String refreshToken = jwtUtil.createRefreshToken(MemberDto.from(member));

        String redisRefreshToken = AuthRedisKey.REFRESH_TOKEN.makeRefreshToken(member.getEmail());

        redisTemplate.opsForValue().set(redisRefreshToken, refreshToken, jwtUtil.getRefreshExpiration(), AuthRedisKey.REFRESH_TOKEN.getTimeUnit());

        return TokenDto.builder().grantType(GrantType.Bearer.getType())
                .accessToken(accessToken).refreshToken(refreshToken).build();

    }

    public TokenDto reissueWithJwt(String token) {

        if (token == null)
            throw new BusinessException(ErrorCode.EMPTY_TOKEN);

        try {
            jwtUtil.validateRefreshToken(token);
        } catch (ExpiredJwtException e) {
            log.info(ErrorCode.EXPIRED_REFRESH_TOKEN.getMessage());
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.info(ErrorCode.MALFORMED_TOKEN.getMessage());
            throw new BusinessException(ErrorCode.MALFORMED_TOKEN);
        }

        String email = jwtUtil.getEmailFromRefreshToken(token);
        String role = jwtUtil.getRoleFromRefreshToken(token);

        String redisRefreshToken = AuthRedisKey.REFRESH_TOKEN.makeRefreshToken(email);
        String savedToken = (String) redisTemplate.opsForValue().get(redisRefreshToken);

        if (savedToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (!token.equals(savedToken)) {
            redisTemplate.delete(redisRefreshToken);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String refreshToken = jwtUtil.createRefreshToken(MemberDto.builder().email(email).role(role).build());
        String accessToken = jwtUtil.createAccessToken(MemberDto.builder().email(email).role(role).build());

        redisTemplate.opsForValue().set(redisRefreshToken, refreshToken, jwtUtil.getRefreshExpiration(), AuthRedisKey.REFRESH_TOKEN.getTimeUnit());

        return TokenDto.builder().grantType(GrantType.Bearer.getType())
                .accessToken(accessToken).refreshToken(refreshToken).build();

    }

}
