package app.project.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {

    private Long id;

    @NotNull
    private Long contentId;

    private Long parentId;

    @NotBlank
    private String text;

}
