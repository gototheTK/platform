package app.project.platform.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class contentImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Content content;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String originalUrl;

    @Builder
    public contentImage(Content content, String fileUrl, String originalUrl) {
        this.content = content;
        this.fileUrl = fileUrl;
        this.originalUrl = originalUrl;
    }

}
