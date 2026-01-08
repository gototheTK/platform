package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ModifyRequestDto {

    @NotNull(message = "{content.error.notFound}")
    private Long id;

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

}
