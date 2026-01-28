package app.project.platform.domain.dto;

import app.project.platform.entity.Content;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ContentResponseDto {

    private Long id;

    private String title;

    private String description;

    private String nickname;

    private String category;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private List<CommentResponseDto> comments;

    private Long likeCount;

    public static ContentResponseDto of (Content content) {
        return ContentResponseDto.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .nickname(content.getAuthor().getNickname())
                .category(content.getCategory().getName())
                .createdDate(content.getCreatedDate())
                .modifiedDate(content.getModifiedDate())
                .comments(content.getComments().stream()
                        .filter(comment->comment.getParent() == null)
                        .map(CommentResponseDto::from)
                        .toList()
                )
                .likeCount(content.getLikeCount())
                .build();
    }

}
