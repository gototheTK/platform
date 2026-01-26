package app.project.platform.repository;

import app.project.platform.entity.Content;
import app.project.platform.entity.ContentLike;
import app.project.platform.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {

    boolean existsByContentAndMember (Content content, Member member);

    void deleteByContentAndMember (Content content, Member member);

}
