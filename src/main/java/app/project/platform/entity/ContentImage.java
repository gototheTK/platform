package app.project.platform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "content_id")
    private Content content;

    private String originalFileName;    // 사용자가 올린 파일명

    private String storeFilename;      // 서버에 저장할 파일명

    @Builder
    public ContentImage(Content content, String originalFileName, String storeFilename) {
        this.content = content;
        this.originalFileName = originalFileName;
        this.storeFilename = storeFilename;
    }

}
