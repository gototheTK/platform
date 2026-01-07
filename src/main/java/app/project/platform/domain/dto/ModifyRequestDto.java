package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ModifyRequestDto {

    @NotBlank(message = "{content.error.notFound}")
    private Long id;

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

}
