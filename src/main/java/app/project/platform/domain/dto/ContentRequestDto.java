package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ContentRequestDto {

    @NotNull(message = "{content.required.id}")
    private Long id;

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

    private String category;

}
