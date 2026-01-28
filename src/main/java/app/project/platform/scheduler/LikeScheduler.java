package app.project.platform.scheduler;

import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class LikeScheduler {

    Logger logger = Logger.getLogger(LikeScheduler.class.toString());

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContentRepository contentRepository;

    //  10초마다 실행 (실무에서는 1분~5분궈장)
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncLikeCount() {

        // 1. Redis에서 "like:count:*" 패턴을 가진 모든 키를 찾습니다.
        Set<String> keys = redisTemplate.keys("like:count:*");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            //  2. 게시글 ID 추출 ("like:count:1" -> "1")
            Long contentId = Long.parseLong(key.split(":")[2]);

            //  3. Redis에서 숫자를 가져오면 '0'으로 리셋합니다. (Atomic 연산: getAndSet)
            //  가져온 숫자만큼 DB에 더해줄 겁니다.
            Integer countObj = (Integer) redisTemplate.opsForValue().getAndSet(key, 0);

            if (countObj == null) continue;

            Long count = Long.valueOf(countObj);


            if (count != 0) {
                contentRepository.updateLikeCount(contentId, count);
                logger.info("동기화 완료: 게시글(" + contentId + ") -> " + count + "개 반영");
            }
        }

    }

}
