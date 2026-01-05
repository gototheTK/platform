package app.project.platform.entity;

import app.project.platform.domain.type.BaseTimeEntity;
import app.project.platform.domain.type.ContentCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentCategory category;

    private Long views;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member author;

    @Builder
    public Content(String title, String description, ContentCategory category, Member author) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.views = 0L;
        this.author = author;
    }

    public void increaseView() {
        this.views++;
    }

}
