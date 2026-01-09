package app.project.platform.domain.dto;

import lombok.Getter;

@Getter
public class CommentRequestDto {

    private Long id;

    private Long contentId;

    private Long parentId;

    private String text;

}
