package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CommentRequestDto {

    private Long id;

    @NotNull
    private Long contentId;

    @NotNull
    private Long parentId;

    @NotBlank
    private String text;

}
