package app.project.platform.repository;

import app.project.platform.entity.Content;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Content c where c.id = :id")
    Optional<Content> findByIdWithLock(@Param("id") Long id);
    **/
    @Modifying(clearAutomatically = true)   // 쿼리 실행 후 영속성 컨텍스트 비우기
    @Query("update Content c set c.likeCount = c.likeCount + :count where c.id = :id")
    void updateLikeCount(@Param("id") Long id, @Param("count") Long count);

}
