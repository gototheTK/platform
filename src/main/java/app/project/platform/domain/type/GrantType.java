package app.project.platform.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrantType {

    Bearer("Bearer");

    private final String type;

}
