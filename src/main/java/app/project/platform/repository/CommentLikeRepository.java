package app.project.platform.repository;

import app.project.platform.entity.Comment;
import app.project.platform.entity.CommentLike;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByContentAndMember(Content content, Member member);

    void deleteByCommentAndMember(Comment comment, Member member);

}
