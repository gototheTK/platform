package app.project.platform.util;

import app.project.platform.domain.dto.MemberDto;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    private final long expiration;

    //  생성자를 통해 yml에 설정한 비밀키와 만료시간을 주입받아 초기화한다.
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }

    //  발급
    public String createToken(MemberDto memberDto) {
        return Jwts.builder()
                .subject(memberDto.getEmail())
                .claim("role", memberDto.getRole())
                .issuedAt(new Date(System.currentTimeMillis())) // 발행 시간
                .expiration(new Date(System.currentTimeMillis() + expiration))  // 만료 시간
                .signWith(secretKey)    //  위조 방지를 위한 서명
                .compact();
    }

    //  조회(이메일)
    public String getEmail(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();  //  아까 subject에 넣었던 이메일을 꺼냄
    }

    // 조회(권한)
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    //  토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            //  만료되었거나, 손상되었거나, 지원되지 않는 토큰일 경우 예외가 발생합니다.
            return false;
        }
    }
}
