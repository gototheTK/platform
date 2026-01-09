package app.project.platform.domain.dto;

import app.project.platform.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {

    private Long id;

    private String text;

    private String authorNickname;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    public static CommentResponseDto from (Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorNickname(comment.getAuthor().getNickname())
                .createdDate(comment.getCreatedDate())
                .modifiedDate(comment.getModifiedDate())
                .build();
    }

}
