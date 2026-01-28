package app.project.platform.entity;


import app.project.platform.domain.type.ContentCategory;
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentCategory category;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ContentImage> files = new ArrayList<>();

    // 좋아요 개수를 저장할 실제 컬럼 (기본값 0)
    @Column(nullable = false)
    private Long likeCount = 0L;

    //  [비즈니스 메서드] 좋아요 증가
    public   void increaseLikeCount() {
        this.likeCount++;
    }

    // [비즈니스 메서드] 좋아요 감소
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    @Builder
    public Content (String title, String description, Member author, ContentCategory category) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.category = category;
    }

    @Builder
    public Content (Long id, String title, String description, Member author, ContentCategory category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.category = category;
    }

    public void update(String title, String description, ContentCategory category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

}
