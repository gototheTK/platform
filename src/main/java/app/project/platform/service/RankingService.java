package app.project.platform.service;

import app.project.platform.domain.RedisKey;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContentRepository contentRepository;

    @Transactional(readOnly = true)
    public List<ContentResponseDto> readDailyRankingContents(int num) {

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String LIKE_CONTENT_RANKING  = RedisKey.LIKE_DAILY_RANKING_COUNT.getPrefix() + today;

        Set<Object> idObjSet = redisTemplate.opsForZSet().reverseRange(LIKE_CONTENT_RANKING, 0, num - 1);

        if (idObjSet == null || idObjSet.isEmpty()) return new ArrayList<>();

        List<Long> idList = idObjSet.stream().map(o -> Long.valueOf(o.toString())).toList();

        Map<Long, Content> contentMap = contentRepository.findAllById(idList).stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        List<ContentResponseDto> sortedContents = new ArrayList<>();

        //  Redis 랭킹 순서대로
        for (Long id : idList) {
            if (contentMap.containsKey(id)) {
                sortedContents.add(ContentResponseDto.of(contentMap.get(id)));
            }
        }

        return sortedContents;

    }

}
