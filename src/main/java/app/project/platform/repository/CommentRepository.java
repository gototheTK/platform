package app.project.platform.repository;

import app.project.platform.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Modifying(clearAutomatically = true)
    @Query("update Comment c set c.likeCount = :count where c.id = :id")
    void updateCommentLikeCount(@Param("id") Long id, @Param("count") Long count);

    @Query("select c.id from Comment c where c.id in :ids")
    List<Long> findExistingIds(List<Long> ids);

}
