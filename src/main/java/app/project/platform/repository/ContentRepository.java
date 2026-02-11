package app.project.platform.repository;

import app.project.platform.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Content c where c.id = :id")
    Optional<Content> findByIdWithLock(@Param("id") Long id);
    **/
    @Query(value = "select c from Content c join fetch c.author",
            countQuery = "select count(c) from Content c")
    Page<Content> findAllWithAuthor(Pageable pageable);

    @Query(value = "select c from Content c join fetch c.author where c.id in :ids")
    List<Content> findAllWithAuthorById(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true)   // 쿼리 실행 후 영속성 컨텍스트 비우기
    @Query(value = "update Content c set c.likeCount = c.likeCount + :count where c.id = :id")
    void updateLikeCount(@Param("id") Long id, @Param("count") Long count);

}
