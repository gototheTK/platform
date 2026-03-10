package app.project.platform.scheduler;

import app.project.platform.domain.RedisKey;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentLike;
import app.project.platform.entity.Member;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    private final MemberRepository memberRepository;

    private final ContentRepository contentRepository;

    private final CommentRepository commentRepository;

    private final ContentLikeRepository contentLikeRepository;

    //  10초마다 실행 (실무에서는 1분~5분궈장)
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_CONTENTS.makeKey();
        String COUNT_KEY_PREFIX = RedisKey.LIKE_CONTENT_COUNT.makeKey();

        //  1. 변경된 게시글 ID들을 팝(Pop)으로 한 번에 가져옵니다.
        //  (pop을 쓰면 가져옴과 동시에 Redis Set에 삭제되므로 중복 처리 방지됨)
        List<Object> contentIds = redisTemplate.opsForSet().pop(DIRTY_KEY, 1000);

        if (contentIds == null || contentIds.isEmpty()) {
            return;
        }

        for (Object idObj : contentIds) {
            Long contentId = Long.valueOf(idObj.toString());
            String redisContentCountKey = COUNT_KEY_PREFIX + contentId;

            Content content = contentRepository.findById(contentId).orElse(null);

            if (content == null) continue;

            //  2. 해당 글의 최신 좋아요 개수 조회 (get)
            Object countObj = redisTemplate.opsForValue().get(redisContentCountKey);

            if (countObj != null) {
                Long finalCount = Long.valueOf(countObj.toString());

                contentRepository.updateLikeCount(contentId, finalCount);

                log.info("동기화 완료: 게시글({}) -> 현재 {}", contentId, finalCount);
            }

            // DB에 좋아요를 INSERT 한다.
            //  Redis에서 좋아요를 누른 유저 ID들을 꺼내옵니다.
            String redisLikeContentUsersKey = RedisKey.LIKE_CONTENT_USERS.makeKey(contentId);

            Long size = redisTemplate.opsForList().size(redisLikeContentUsersKey);

            if (size == null) continue;

            // DB 적재 횟수
            long unit = 1000;
            long times = size/unit + size%unit==0 ? 0 : 1;

            for(int i=0; i<times; i++) {

                List<Object> memberIds = redisTemplate.opsForList().leftPop(redisLikeContentUsersKey, unit);

                if (memberIds == null || memberIds.isEmpty()) break;

                for (Object memberIdObj : memberIds) {

                    Long memberId = Long.valueOf(memberIdObj.toString());

                    // 1. 껍데기(Proxy) 객체 생성! (DB SELECT 쿼리 발생 안함)
                    Member proxyMember = memberRepository.getReferenceById(memberId);

                    // 2. 엔티티 조립
                    ContentLike contentLike = ContentLike.builder()
                            .content(content)
                            .member(proxyMember)
                            .build();

                    // 3. DB INSERT 쿼리


                    // 4. 유저별 카운트 증가
                    String MEMBER_CATEGORY_LIKE_COUNT = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(memberId);
                    redisTemplate.opsForHash().increment(MEMBER_CATEGORY_LIKE_COUNT, content.getCategory(), 1);

                }

                //  5. DB INSERT 쿼리

            }

            log.info("좋아요! 갱신 성공!");
        }

    }

    //  10초 마다 실행
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncCommentLikeCount() {

        String DIRTY_KEY = RedisKey.LIKE_UPDATED_COMMENTS.getPattern();
        String COUNT_KEY_PREFIX = RedisKey.LIKE_COMMENT_COUNT.getPattern();

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
