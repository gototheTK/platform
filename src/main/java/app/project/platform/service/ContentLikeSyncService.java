package app.project.platform.service;

import app.project.platform.domain.RedisKey;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentLikeSyncService {

    private final ContentRepository contentRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private final JdbcTemplate jdbcTemplate;

    // 개별 댓글 동기화 (여기서만 DB 트랜잭션을 보장한다.)
    @Transactional
    public void processSingleContentSync(Content content) {

        //  1. 해당 글의 좋아요 수 동기화
        String redisContentCountKey = RedisKey.LIKE_CONTENT_COUNT.makeKey(content.getId());
        Object countObj = redisTemplate.opsForValue().get(redisContentCountKey);

        if (countObj != null) {
            Long likeCount = Long.valueOf(countObj.toString());

            contentRepository.updateLikeCount(content.getId(), likeCount);

            log.info("동기화 완료: 게시글({}) 좋아요 -> 현재 {}", content.getId(), likeCount);
        }

        // 2. 좋아요 추가 큐 동기화
        // DB 적재 횟수
        long unit = 1000;
        long start = 0;
        long end = unit-1;

        String redisLikeContentUsersKey = RedisKey.LIKE_CONTENT_USERS_QUEUE.makeKey(content.getId());
        String insertSql = "INSERT INTO content_like (content_id, member_id) VALUES(?, ?)";

        List<Object> memberIdsToInsert = redisTemplate.opsForList().range(redisLikeContentUsersKey, start, end);
        while (memberIdsToInsert != null && !memberIdsToInsert.isEmpty()) {

            // batchUpdate
            // INSERT 처리
            jdbcTemplate.batchUpdate(insertSql, memberIdsToInsert, memberIdsToInsert.size(), (ps, memberObj) -> {

                long memberId = Long.parseLong(memberObj.toString());

                ps.setLong(1, content.getId());
                ps.setLong(2, memberId);
            });

            for (Object memberObj : memberIdsToInsert) {

                Long memberId = Long.valueOf(memberObj.toString());

                // 유저별 카운트 증가
                String MEMBER_CATEGORY_LIKE_COUNT = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(memberId);
                redisTemplate.opsForHash().increment(MEMBER_CATEGORY_LIKE_COUNT, content.getCategory(), 1);
            }

            redisTemplate.opsForList().trim(redisContentCountKey, memberIdsToInsert.size(), -1);
            memberIdsToInsert = redisTemplate.opsForList().range(redisLikeContentUsersKey, start, end);

        }

        String redisLikeContentUsersRemoveQueue = RedisKey.LIKE_CONTENT_USERS_REMOVE_QUEUE.makeKey(content.getId());
        String deleteSql = "DELETE FROM content_like WHERE content_id = ? AND member_id = ?";

        List<Object> memberIdsToRemove = redisTemplate.opsForList().range(redisLikeContentUsersRemoveQueue, start, end);
        while (memberIdsToRemove != null && !memberIdsToRemove.isEmpty()) {

            // batchUpdate
            // DELETE 처리
            jdbcTemplate.batchUpdate(deleteSql, memberIdsToRemove, memberIdsToRemove.size(), (ps, memberObj) -> {

                long memberId = Long.parseLong(memberObj.toString());

                ps.setLong(1, content.getId());
                ps.setLong(2, memberId);
            });

            for (Object memberObj : memberIdsToRemove) {

                Long memberId = Long.valueOf(memberObj.toString());

                // 유저별 카운트 증가
                String MEMBER_CATEGORY_LIKE_COUNT = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(memberId);
                redisTemplate.opsForHash().increment(MEMBER_CATEGORY_LIKE_COUNT, content.getCategory(), -1);
            }

            redisTemplate.opsForList().trim(redisLikeContentUsersRemoveQueue, memberIdsToRemove.size(), -1);
            memberIdsToRemove = redisTemplate.opsForList().range(redisLikeContentUsersRemoveQueue, start, end);
        }

    }

}
