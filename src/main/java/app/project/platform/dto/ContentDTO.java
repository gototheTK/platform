package app.project.platform.dto;

import app.project.platform.domain.type.ContentCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentDTO {

    private Long id;

    @NotBlank
    private ContentCategory contentCategory;

    private int page = 0;

}
