package app.project.platform.domain.dto;

import app.project.platform.entity.Content;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContentDto {

    private Long id;

    private String title;

    private String description;

    private String nickname;

    private Long categoryId;

    private LocalDateTime createdDate;

    private LocalDateTime lastModifiedDate;

    public static ContentDto from (Content content) {
       return ContentDto.builder()
               .id(content.getId())
               .title(content.getTitle())
               .description(content.getDescription())
               .nickname(content.getAuthor().getNickname())
               .createdDate(content.getCreatedDate())
               .lastModifiedDate(content.getModifiedDate())
               .build();
    }

}
