package app.project.platform.service;

import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RankingServiceTest {

    @InjectMocks
    RankingService rankingService;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    @Mock
    ContentRepository contentRepository;

    @Test
    @DisplayName("랭킹 조회 성공")
    void 랭킹_조회_성공 () {

        // 테스트 데이터 세팅
        int num = 10;
        Long firstPlaceId = 3L;
        Long secondPlaceId = 1L;

        Content content1 = Content.builder().build();
        ReflectionTestUtils.setField(content1, "id", firstPlaceId);

        Content content3 = Content.builder().build();
        ReflectionTestUtils.setField(content3, "id", secondPlaceId);

        Set<Object> rankIds = new LinkedHashSet<>();
        rankIds.add(firstPlaceId);
        rankIds.add(secondPlaceId);

        //  given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong())).willReturn(rankIds);
        given(contentRepository.findAllById(anyList())).willReturn(List.of(content3, content1));

        //  when
        List<ContentResponseDto> result = rankingService.readDailyRankingContents(num);

        //  then
        assertThat(result).hasSize(2);

        // 랭킹 일치 확인
        assertThat(result.get(0).getId()).isEqualTo(firstPlaceId);
        assertThat(result.get(1).getId()).isEqualTo(secondPlaceId);

        verify(redisTemplate, times(1)).opsForZSet();
        verify(zSetOperations, times(1)).reverseRange(anyString(), anyLong(), anyLong());
        verify(contentRepository, times(1)).findAllById(anyList());

    }

    @Test
    @DisplayName("랭킹 조회 실패")
    void 랭킹_조회_실패_빈_리스트 () {
        
        //  테스트 데이터 세팅
        int num = 10;

        //  given
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.reverseRange(anyString(), anyLong(), anyLong())).willReturn(new LinkedHashSet<>());

        //  when
        List<ContentResponseDto> result = rankingService.readDailyRankingContents(num);

        //  then
        assertThat(result).hasSize(0);

        verify(redisTemplate, times(1)).opsForZSet();
        verify(zSetOperations, times(1)).reverseRange(anyString(), anyLong(), anyLong());
        verify(contentRepository, times(0)).findAllById(anyList());

    }


}
