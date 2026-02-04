package app.project.platform.scheduler;

import app.project.platform.domain.RedisKey;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

    //  10초마다 실행 (실무에서는 1분~5분궈장)
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_CONTENTS.getPrefix();
        String COUNT_KEY_PREFIX = RedisKey.LIKE_CONTENT_COUNT.getPrefix();

        //  1. 변경된 게시글 ID들을 팝(Pop)으로 한 번에 가져옵니다.
        //  (pop을 쓰면 가져옴과 동시에 Redis Set에 삭제되므로 중복 처리 방지됨)
        List<Object> contentIds = redisTemplate.opsForSet().pop(DIRTY_KEY, 1000);

        if (contentIds == null || contentIds.isEmpty()) {
            return;
        }

        for (Object idObj : contentIds) {
            Long contentId = Long.valueOf(idObj.toString());
            String countKey = COUNT_KEY_PREFIX + contentId;

            //  2. 해당 글의 최신 좋아요 개수 조회 (get)
            Object countObj = redisTemplate.opsForValue().get(countKey);

            if (countObj != null) {
                Long finalCount = Long.valueOf(countObj.toString());

                contentRepository.updateLikeCount(contentId, finalCount);

                log.info("동기화 완료: 게시글({}) -> 현재 {}", contentId, finalCount);
            }

        }

    }

    //  10초 마다 실행
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncCommentLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_COMMENTS.getPrefix();
        String COUNT_KEY_PREFIX = RedisKey.LIKE_COMMENT_COUNT.getPrefix();

        //  1.  변경 된 ID들을 팝으로 가져옵니다.
        List<Object> commentIds = redisTemplate.opsForSet().pop(DIRTY_KEY, 1000);

        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }

        for (Object idObj : commentIds) {

            Long commentId = Long.valueOf(idObj.toString());
            String countKey= COUNT_KEY_PREFIX + commentId;

            Object countObj = redisTemplate.opsForValue().get(countKey);

            if (countObj != null) {

                Long likeCount = Long.valueOf(countObj.toString());
                commentRepository.updateCommentLikeCount(commentId, likeCount);

                log.info("동기화 완료: 댓글 ({}) -> 현재 {}", commentId, likeCount);

            }

        }

    }

}
