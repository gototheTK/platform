package app.project.platform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "comment_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_member",
                        columnNames = {"comment_id", "member_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CommentLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public CommentLike(Comment comment, Member member) {
        this.comment = comment;
        this.member = member;
    }

}
