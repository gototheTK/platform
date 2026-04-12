package app.project.platform.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public enum SchedulerLockKey {


    CONTENT_LIKE_SYNC_LOCK("scheduler:content:like_sync_lock", 0, 3, TimeUnit.MINUTES),
    COMMENT_LIKE_SYNC_LOCK("scheduler:comment:like_sync_lock", 0,3, TimeUnit.MINUTES),
    ;

    private final String pattern;
    private final long waitTime;
    private final long leaseTime;
    private final TimeUnit timeUnit;

}
