package app.project.platform.util;

import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.TokenDto;
import app.project.platform.entity.Member;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey accessSecretKey;

    private final SecretKey refreshSecretKey;

    private final long accessExpiration;

    private final long refreshExpiration;

    //  생성자를 통해 yml에 설정한 비밀키와 만료시간을 주입받아 초기화한다.
    public JwtUtil(@Value("${jwt.accessSecretKey}") String accessSecretKey,
                   @Value("${jwt.refreshSecretKey}") String refreshSecretKey,
                   @Value("${jwt.access-expiration}") String accessExpiration,
                   @Value("${jwt.refresh-expiration}") String refreshExpiration) {
        byte[] accessKeyBytes = Decoders.BASE64.decode(accessSecretKey);
        byte[] secretKeyBytes = Decoders.BASE64.decode(refreshSecretKey);
        this.accessSecretKey = Keys.hmacShaKeyFor(accessKeyBytes);
        this.refreshSecretKey = Keys.hmacShaKeyFor(secretKeyBytes);
        this.accessExpiration = Long.parseLong(accessExpiration);
        this.refreshExpiration = Long.parseLong(refreshExpiration);
    }

    //  엑세스 토큰 발급
    public String createAccessToken(MemberDto memberDto) {
        return Jwts.builder()
                .subject(memberDto.getEmail())
                .claim("role", memberDto.getRole())
                .issuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))  // 만료 시간
                .signWith(accessSecretKey)    //  위조 방지를 위한 서명
                .compact();
    }

    //  리프레시 토큰 발급
    public String createRefreshToken(MemberDto memberDto) {
        return Jwts.builder()
                .subject(memberDto.getEmail())
                .claim("role", memberDto.getRole())
                .issuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))  // 만료 시간
                .signWith(refreshSecretKey)    //  위조 방지를 위한 서명
                .compact();
    }

    //  조회(이메일)
    public String getEmailFromAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();  //  아까 subject에 넣었던 이메일을 꺼냄
    }

    //  조회(권한)
    public String getRoleFromAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    //  조회(이메일)
    public String getEmailFromRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();  //  아까 subject에 넣었던 이메일을 꺼냄
    }

    //  조회(권한)
    public String getRoleFromRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    //  엑세스 토큰 유효성 검증
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(accessSecretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            //  만료되었거나, 손상되었거나, 지원되지 않는 토큰일 경우 예외가 발생합니다.
            return false;
        }
    }

    //  리프레시 토큰 유효성 검증
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser().verifyWith(refreshSecretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            //  만료되었거나, 손상되었거나, 지원되지 않는 토큰일 경우 예외가 발생합니다.
            return false;
        }
    }

}
