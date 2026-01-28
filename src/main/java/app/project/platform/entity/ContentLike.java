package app.project.platform.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "content_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_content_member",
                        columnNames = {"content_id", "member_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public ContentLike(Content content, Member member) {
        this.content = content;
        this.member = member;
    }

}