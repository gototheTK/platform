package app.project.platform.service;

import app.project.platform.domain.PostRedisKey;
import app.project.platform.repository.CommentRepository;
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
public class CommentLikeSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentRepository commentRepository;
    private final JdbcTemplate jdbcTemplate;

    // 개별 댓글 동기화 (여기서만 DB 트랜잭션을 보장한다.)
    @Transactional
    public void processSingleCommentSync(Long commentId) {

        //  1. 총 좋아요 수 동기화
        String redisLikeCommentCount = PostRedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);
        Object countObj = redisTemplate.opsForValue().get(redisLikeCommentCount);

        if (countObj != null) {
            Long likeCount = Long.valueOf(countObj.toString());
            commentRepository.updateCommentLikeCount(commentId, likeCount);

            log.info("동기화 완료: 댓글({}) 좋아요  -> 현재 {}", commentId, likeCount);
        }

        // 2. 종아효 추가 큐 동기화 (Batch Insert)
        // 데이터들을 DB에 반영한다.

        long unit = 1000;
        long start = 0;
        long end = unit-1;

        String redisLikeCommentUsersQueue = PostRedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);
        String insert = "INSERT INTO comment_like (comment_id, member_id) VALUES(?, ?)";

        List<Object> memberIds = redisTemplate.opsForList().range(redisLikeCommentUsersQueue, start, end);
        while (memberIds != null && !memberIds.isEmpty()) {

            // 배치 업데이트
            jdbcTemplate.batchUpdate(insert, memberIds, memberIds.size(), (ps, memberObj) -> {

                long memberId = Long.parseLong(memberObj.toString());

                //  INSERT 처리
                ps.setLong(1, commentId);
                ps.setLong(2, memberId);
            });

            redisTemplate.opsForList().trim(redisLikeCommentUsersQueue, memberIds.size(), -1);
            memberIds = redisTemplate.opsForList().range(redisLikeCommentUsersQueue, start, end);

        }

        // 3. 좋아요 취소 큐 동기화 (Batch Delete)
        String redisLikeCommentUsersRemoveQueue = PostRedisKey.LIKE_COMMENT_USERS_REMOVE_QUEUE.makeKey(commentId);
        String delete = "DELETE FROM comment_like WHERE comment_id = ? AND member_id = ?";

        memberIds = redisTemplate.opsForList().range(redisLikeCommentUsersRemoveQueue, start, end);
        while (memberIds != null && !memberIds.isEmpty()) {

            //  배치 업데이트
            jdbcTemplate.batchUpdate(delete, memberIds, memberIds.size(), (ps, memberObj) -> {

                long memberId = Long.parseLong(memberObj.toString());

                //  DELETE 처리
                ps.setLong(1, commentId);
                ps.setLong(2, memberId);

            });

            redisTemplate.opsForList().trim(redisLikeCommentUsersRemoveQueue, memberIds.size(), -1);
            memberIds = redisTemplate.opsForList().range(redisLikeCommentUsersRemoveQueue, start, end);

        }

    }

}
