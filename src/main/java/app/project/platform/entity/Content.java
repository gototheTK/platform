package app.project.platform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends DateTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Member author;

    @Builder
    public Content(String title, String description, Member author) {
        this.title = title;
        this.description = description;
        this.author = author;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

}
