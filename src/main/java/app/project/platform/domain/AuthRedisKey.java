package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public enum AuthRedisKey {

    // 인강 & 인가
    REFRESH_TOKEN("auth:member:refresh:token:%s", TimeUnit.MILLISECONDS),
    ;

    private final String pattern;
    private final TimeUnit timeUnit;

    public String makeRefreshToken(String email) {
        return String.format(pattern, email);
    }

}
