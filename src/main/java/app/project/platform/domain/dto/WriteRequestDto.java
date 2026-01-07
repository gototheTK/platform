package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class WriteRequestDto {

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

}
