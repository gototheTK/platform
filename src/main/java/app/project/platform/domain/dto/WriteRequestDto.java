package app.project.platform.domain.dto;

import app.project.platform.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class WriteRequestDto {

    @NotBlank(message = "{content.required.title}")
    private String title;

    private String description;

    @NotNull
    private Long categoryId;

}
