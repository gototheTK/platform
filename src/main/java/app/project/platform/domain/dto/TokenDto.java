package app.project.platform.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenDto {

    //  인증 타입 (보통 "Bearer" 사용)
    private String grantType;

    //  실제 사용할 JWT 토큰
    private String accessToken;

    //  만료시간
    private Long accessTokenExpiration;

    //  리프레시 토큰
    private String refreshToken;
}
