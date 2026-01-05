package app.project.platform.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentCategory {

    WEBTOON("웹툰"),
    NOVEL("소설"),
    ILLUST("일러스트");

    private final String description;

}
