package app.project.platform.domain.dto;

import app.project.platform.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentResponseDto {

    private Long id;

    private String text;

    private String author;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private List<CommentResponseDto> comments;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(comment.getAuthor().getNickname())
                .createdDate(comment.getCreatedDate())
                .modifiedDate(comment.getModifiedDate())
                .comments(comment.getChildren().stream()
                        .map(CommentResponseDto::from)
                        .toList())
                .build();
    }

}
