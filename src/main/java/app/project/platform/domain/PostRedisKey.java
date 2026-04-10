package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public enum PostRedisKey {

    //  글 조회
    VALID_CONTENTS("post:content:valid", 7, TimeUnit.DAYS),
    VALID_COMMENTS("post:comment:valid", 7, TimeUnit.DAYS),

    //  글 좋아요
    LIKE_CONTENT_USERS("post:like:content:users:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_USERS_QUEUE("post:like:content:users:queue:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_USERS_REMOVE_QUEUE("post:like:content:users:remove:queue:%s", 7, TimeUnit.DAYS),
    LIKE_CONTENT_COUNT("post:like:content:count:%s", 7, TimeUnit.DAYS),
    LIKE_UPDATED_CONTENTS("post:like:updated:contents", 7, TimeUnit.DAYS),
    LIKE_DAILY_RANKING_COUNT("post:like:ranking:daily:count:%s", 7, TimeUnit.DAYS),

    // 댓글 좋아요
    LIKE_COMMENT_USERS_SET("post:comment:users:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_USERS_QUEUE("post:like:comment:users:queue:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_USERS_REMOVE_QUEUE("post:like:comment:users:remove:queue:%s", 7, TimeUnit.DAYS),
    LIKE_COMMENT_COUNT("post:like:comment:count:%s", 7, TimeUnit.DAYS),
    LIKE_UPDATED_COMMENTS("post:like:updated:comments", 7, TimeUnit.DAYS),

    // 회원 취향 조사(Hash)
    MEMBER_CATEGORY_VIEW_COUNT("post:member:category:view:count:%s", 7, TimeUnit.DAYS),
    MEMBER_CATEGORY_LIKE_COUNT("post:member:category:like:count:%s", 7, TimeUnit.DAYS),

    // 조회수 어뷰링 방지 로그 (String)
    VIEW_LOG("post:log:user:%s:view:%s", 10, TimeUnit.MINUTES),

    // 추천 피드 커서
    FEED_BUFFER("post:feed:buffer:%s", 60, TimeUnit.MINUTES),

    //  피드 커서
    FEED_CURSOR("post:feed:cursor:%s", 60, TimeUnit.MINUTES)

    ;

    private final String pattern;
    private final long ttl;
    private  final TimeUnit timeUnit;

    public String makeKey(Object... args) {
        return String.format(pattern, args);
    }

}
