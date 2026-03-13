package app.project.platform.scheduler;

import app.project.platform.domain.RedisKey;
import app.project.platform.entity.Content;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import app.project.platform.service.CommentLikeSyncService;
import app.project.platform.service.ContentLikeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContentRepository contentRepository;

    private final CommentRepository commentRepository;

    private final ContentLikeSyncService contentLikeSyncService;

    private final CommentLikeSyncService commentLikeSyncService;

    //  10초마다 실행 (실무에서는 1분~5분궈장)
    @Transactional
    public void syncLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_CONTENTS.makeKey();

        //  1. 변경된 게시글 ID들을 팝(Pop)으로 한 번에 가져옵니다.
        //  (pop을 쓰면 가져옴과 동시에 Redis Set에 삭제되므로 중복 처리 방지됨)
        List<Object> poppedIds = redisTemplate.opsForSet().pop(DIRTY_KEY, 1000);

        if (poppedIds == null || poppedIds.isEmpty()) {
            return;
        }

        List<Long> contentIds = poppedIds.stream().map(o->Long.valueOf(o.toString())).toList();

        // 존재하는 글의 아이디만 가져온다.
        List<Content> validContentIds = contentRepository.findExistingContentsById(contentIds);

        for (Content content : validContentIds) {

            try {
                contentLikeSyncService.processSingleContentSync(content);
            } catch (Exception e) {
                log.error("글({}) 좋아요 동기화 중 에러 발생: {}", content.getId(), e.getMessage());
                redisTemplate.opsForSet().add(DIRTY_KEY, content.getId());
            }

        }

        log.info("글 좋아요 동기화 성공!");

    }

    //  10초 마다 실행
    @Scheduled(fixedDelay = 10000)
    public void syncCommentLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_COMMENTS.makeKey();

        //  1.  변경 된 ID들을 팝으로 가져옵니다.
        List<Object> poppedIds = redisTemplate.opsForSet().pop(DIRTY_KEY, 1000);

        if (poppedIds == null || poppedIds.isEmpty()) {
            return;
        }

        List<Long> commentIds = poppedIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();

        List<Long> validCommentIds = commentRepository.findExistingIds(commentIds);

        // 2. 댓글의 좋아요 수를 DB에 반영한다.
        for (Long commentId : validCommentIds) {

            try {
                commentLikeSyncService.processSingleCommentSync(commentId);
            } catch (Exception e) {
                log.error("댓글({}) 좋아요 동기화 중 에러 발생: {}", commentId, e.getMessage());
                redisTemplate.opsForSet().add(DIRTY_KEY, commentId);
            }

        }

        log.info("댓글 좋아요 동기화 완료: 총 {}건 처리", validCommentIds.size());

    }

}
