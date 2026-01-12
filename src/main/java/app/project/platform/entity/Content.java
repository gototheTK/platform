package app.project.platform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Member author;

    private String thumbnail_url;

    @OneToMany(orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(orphanRemoval = true)
    private List<ContentImage> contentImages = new ArrayList<>();

    @Builder
    public Content(String title, String description, Member author, Category category, String thumbnail_url) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.category = category;
    }

    public void update(String title, String description, Category category) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.thumbnail_url = thumbnail_url;
    }

}
