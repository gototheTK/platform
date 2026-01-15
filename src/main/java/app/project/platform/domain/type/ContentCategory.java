package app.project.platform.domain.type;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentCategory {

    NOVEL("소설", 1),
    CARTOON("만화", 2);

    private final String name;
    private final int code;

    // Enum 값을 원하는 타입등으로 받기 위해서 쓴다.
    @JsonCreator
    public static ContentCategory from(String inputValue) {
        for (ContentCategory category : ContentCategory.values()) {
            
            if (category.getName().equals(inputValue) || category.name().equals(inputValue)) {
                return category;
            }

        }

        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

}
