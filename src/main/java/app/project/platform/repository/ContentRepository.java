package app.project.platform.repository;

import app.project.platform.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    // Top 3 by createdDate Descending (최신순 3개)
    List<Content> findTop3ByOrderByCreatedDateDesc();

}
