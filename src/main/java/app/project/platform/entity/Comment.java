package app.project.platform.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 1000)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // mappedBy : 부모 객체의 매핑 멤버
    // cascade : Entity로 DB반영시 부모와 자식의 영향 관계
    // orphanRemoval : 자식을 모아놓은 객체에서 요소를 삭제하면 DB에 영향을 주는지 안주는지 정할 때 사용
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children;

    @Builder
    public Comment(Member author, Content content, String text, Comment parent) {
        this.author = author;
        this.content = content;
        this.text = text;
        this.parent = parent;
    }
    
    // 수정 편의 메서드
    public void update(String text) {
        this.text = text;
    }

}
