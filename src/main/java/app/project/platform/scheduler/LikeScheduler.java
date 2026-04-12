package app.project.platform.scheduler;

import app.project.platform.domain.PostRedisKey;
import app.project.platform.domain.SchedulerLockKey;
import app.project.platform.entity.Content;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.service.CommentLikeSyncService;
import app.project.platform.service.ContentLikeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContentRepository contentRepository;

    private final CommentRepository commentRepository;

    private final ContentLikeSyncService contentLikeSyncService;

    private final CommentLikeSyncService commentLikeSyncService;

    private final RedissonClient redissonClient;

    //  10초마다 실행 (실무에서는 1분~5분궈장)
    @Scheduled(fixedDelay = 10000)
    public void syncContentLikeCount() {

        //  1. [핵심] Redis 데이터를 건드리기 전에, '스케줄러 전체'에 대한 분산락을 먼저 시도한다.
        RLock lock = redissonClient.getLock(SchedulerLockKey.CONTENT_LIKE_SYNC_LOCK.getPattern());

        // 처리할 개수
        long count = 1000;

        try {

            // 2. 락 획득 시도 (대기 0초, 임대 3분)
            // 대기시간이 0초인 이유: 다른 서버가 이미 락을 잡았다면 기다릴 필요 없이 즉시 포기(return)하기 위함
            boolean isLocked = lock.tryLock(SchedulerLockKey.CONTENT_LIKE_SYNC_LOCK.getWaitTime(), SchedulerLockKey.CONTENT_LIKE_SYNC_LOCK.getLeaseTime(), TimeUnit.MINUTES);

            if (!isLocked) {
                log.info("다른 서버 인스턴스에서 이미 좋아요 동기화를 진행 중입니다.");
                return;
            }

            log.info("스케줄러 분산락 획득! Redis -> DB 좋아요 동기화를 시작합니다.");

            String DIRTY_KEY = PostRedisKey.LIKE_UPDATED_CONTENTS.makeKey();
            String PROCESSING_KEY = PostRedisKey.LIKE_UPDATED_CONTENTS_PROCESSING.makeKey();

            //  1. Atomic Swap (이름 바꾸기) - Race Condition 차단
            if (Boolean.FALSE.equals(redisTemplate.hasKey(PROCESSING_KEY))) {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_KEY))) {
                    redisTemplate.rename(DIRTY_KEY, PROCESSING_KEY);
                }else {
                    return;
                }
            }

            //  2. 1000개씩 끊어서 처리하는 무한루프(OOM과 유실을 방지)
            while (true) {

                //  1. 혹시 이전 스케줄러가 작업 중 서버가 뻗어서 '처리 중' 데이터가 있는지 확인
                Set<Object> chunkIds = redisTemplate.opsForSet().distinctRandomMembers(PROCESSING_KEY, count);

                if (chunkIds == null || chunkIds.isEmpty()) break;

                // 존재하는 글의 아이디만 가져온다.
                List<Long> contentIds = chunkIds.stream().map(o->Long.valueOf(o.toString())).toList();
                List<Content> validContentIds = contentRepository.findExistingContentsById(contentIds);

                for (Content content : validContentIds) {

                    try {
                        contentLikeSyncService.processSingleContentSync(content);
                    } catch (Exception e) {
                        log.error("글({}) 좋아요 동기화 중 에러 발생: {}", content.getId(), e.getMessage());
                    }

                }

                // 성공했으면 안전하게 버린다.
                redisTemplate.opsForSet().remove(PROCESSING_KEY, chunkIds.toArray());
                log.info("글 좋아요 동기화 성공! {} 개 중 처리된 건수: {}", count, validContentIds.size());

            }

            log.info("모든 글의 좋아요 동기화가 안전하게 완료되었습니다!");
        } catch (InterruptedException e) {
            log.error("분산락 획득 중 인터럽트 예외 발생", e);
            Thread.currentThread().interrupt(); // 스레드 인터럽트 상태 복구
        } finally {
            //  3. 로직이 끝났거나 예외가 발생했을 때, 내가 잡은 락인지 확인하고 해제
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("글 좋아요 동기화 완료. 분산락을 해제합니다.");
            }
        }
    }

    //  10초 마다 실행
    @Scheduled(fixedDelay = 10000)
    public void syncCommentLikeCount() {

        // 락선언
        RLock lock = redissonClient.getLock(SchedulerLockKey.COMMENT_LIKE_SYNC_LOCK.getPattern());

        try {

            // 2. 락 획득 시도 (대기 0초, 임대 3분)
            // 대기시간이 0초인 이유: 다른 서버가 이미 락을 잡았다면 기다릴 필요 없이 즉시 포기(return)하기 위함
            boolean isLocked = lock.tryLock(SchedulerLockKey.COMMENT_LIKE_SYNC_LOCK.getWaitTime(), SchedulerLockKey.COMMENT_LIKE_SYNC_LOCK.getLeaseTime(), SchedulerLockKey.COMMENT_LIKE_SYNC_LOCK.getTimeUnit());

            long count = 1000;

            // 락이 있으면 포기한다.
            if (!isLocked) {
                return;
            }

            // 락이 없다면, 이전에 프로세싱 중인 것들을 꺼내온다.
            String DIRTY_KEY = PostRedisKey.LIKE_UPDATED_COMMENTS.makeKey();
            String PROCESSING_KEY = PostRedisKey.LIKE_UPDATED_COMMENTS_PROCESSING.makeKey();


            // 1. Atomic Swap (이름 바꾸기) - Race Condition 차단
            if (Boolean.FALSE.equals(redisTemplate.hasKey(PROCESSING_KEY))) {
                if (Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_KEY))) {
                    redisTemplate.rename(DIRTY_KEY, PROCESSING_KEY);
                }else {
                    return;
                }
            }

            while (true) {

                Set<Object> chunkIds = redisTemplate.opsForSet().distinctRandomMembers(PROCESSING_KEY, 1000);

                if (chunkIds == null || chunkIds.isEmpty()) break;

                List<Long> commentsIds = chunkIds.stream().map(id -> Long.valueOf(id.toString())).toList();
                List<Long> validCommentIds = commentRepository.findExistingIds(commentsIds);

                //  오류를 대처하면서 동기화를 시도한다.
                for (Long commentId : validCommentIds) {

                    try {
                        commentLikeSyncService.processSingleCommentSync(commentId);
                    }catch (Exception e) {
                        log.error("댓글({}) 좋아요 동기화 중 에러 발생: {}", commentId, e.getMessage());
                    }

                }

                // 성공했으면, 안전하게 버린다.
                redisTemplate.opsForSet().remove(PROCESSING_KEY, chunkIds.toArray());
                log.info("댓글 좋아요 동기화 성공! {} 개 중 처리된 건수: {}", count, validCommentIds.size());

            }

            log.info("모든 댓글의 좋아요 동기화가 안전하게 완료되었습니다!");
        } catch (InterruptedException e) {
            log.error("분산락 획득 중 인터럽트 예외 발생", e);
        } finally {
            //  로직이 끝났거나 예외가 발생했을 때, 내가 잡은 락인지 확인하고 해제
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("댓글 좋아요 동기화 완료. 분산락을 해제합니다.");
            }

        }

    }

}
