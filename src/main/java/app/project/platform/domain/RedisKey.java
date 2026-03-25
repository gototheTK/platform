package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public enum RedisKey {

    //  글 조회
    VALID_CONTENTS("content:valid", 7, TimeUnit.DAYS),
    VALID_COMMENTS("comment:valid", 7, TimeUnit.DAYS),

    //  글 좋아요
    LIKE_CONTENT_USERS("like:content:users:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_USERS_QUEUE("like:content:users:queue:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_USERS_REMOVE_QUEUE("like:content:users:remove:queue:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_COUNT("like:content:count:%s", 7, TimeUnit.DAYS),
    LIKE_UPDATED_CONTENTS("like:updated:contents", 7, TimeUnit.DAYS),
    LIKE_DAILY_RANKING_COUNT("like:ranking:daily:count:%s", 7, TimeUnit.DAYS),

    // 댓글 좋아요
    LIKE_COMMENT_USERS_SET("like:comment:users:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_USERS_QUEUE("like:comment:users:queue:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_USERS_REMOVE_QUEUE("like:comment:users:remove:queue:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_COUNT("like:comment:count:%s", 7, TimeUnit.DAYS),
    LIKE_UPDATED_COMMENTS("like:updated:comments", 7, TimeUnit.DAYS),

    // 회원 취향 조사(Hash)
    MEMBER_CATEGORY_VIEW_COUNT("member:category:view:count:%s", 7, TimeUnit.DAYS),
    MEMBER_CATEGORY_LIKE_COUNT("member:category:like:count:%s", 7, TimeUnit.DAYS),

    // 조회수 어뷰링 방지 로그 (String)
    VIEW_LOG("log:user:%s:view:%s", 10, TimeUnit.MINUTES),

    // 추천 피드 커서
    FEED_BUFFER("feed:buffer:%s", 60, TimeUnit.MINUTES),

    //  피드 커서
    FEED_CURSOR("feed:cursor:%s", 60, TimeUnit.MINUTES)

    ;

    private final String pattern;
    private final long ttl;
    private  final TimeUnit timeUnit;

    public String makeKey(Object... args) {
        return String.format(pattern, args);
    }

}
