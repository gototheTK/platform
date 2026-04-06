package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Word {

    RefreshToken("RefreshToken"),
    AccessToken("AccessToken"),
    Strict("Strict"),
    Role("role"),
    ;

    private final String word;

}
