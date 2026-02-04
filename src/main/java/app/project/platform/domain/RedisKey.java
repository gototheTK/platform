package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedisKey {

    //  글 좋아요
    LIKE_CONTENT_USERS("like:content:users:"),
    LIKE_CONTENT_COUNT("like:content:count:"),
    LIKE_UPDATED_CONTENTS("like:updated:contents"),
    LIKE_DAILY_RANKING_COUNT("like:ranking:daily:count:"),

    LIKE_COMMENT_USERS("like:comment:users:"),
    LIKE_COMMENT_COUNT("like:comment:count:"),
    LIKE_UPDATED_COMMENTS("like:updated:comments");

    private final String prefix;

    public String makeKey(Long id) {
        return this.prefix + id;
    }

}
