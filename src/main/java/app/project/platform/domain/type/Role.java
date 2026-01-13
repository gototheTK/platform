package app.project.platform.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN ("ADMIN", "ROLE_ADMIN"),
    USER ("USER", "ROLE_USER");

    private final String name;
    private final String key;

}
