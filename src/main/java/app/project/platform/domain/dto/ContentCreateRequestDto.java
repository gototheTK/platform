package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentCreateRequestDto {

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

    @NotBlank(message = "{content.required.category}")
    private String category;

}
