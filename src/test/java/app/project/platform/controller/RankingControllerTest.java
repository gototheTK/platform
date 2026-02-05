package app.project.platform.controller;

import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.service.RankingService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RankingController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RankingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RankingService rankingService;

    @Test
    @DisplayName("랭킹 조회 성공")
    void 랭킹_조회_성공 () throws Exception {

        int rankingNum = 10;

        ContentResponseDto content1 = ContentResponseDto.builder().id(200L).build();

        ContentResponseDto content2 = ContentResponseDto.builder().id(100L).build();

        ContentResponseDto content3 = ContentResponseDto.builder().id(150L).build();

        List<ContentResponseDto> result = List.of(content1, content2, content3);

        //  given
        given(rankingService.readDailyRankingContents(rankingNum)).willReturn(result);

        //  when & then
        mockMvc.perform(get("/api/v1/ranking/top/"+rankingNum)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(result.size())))
            .andExpect(jsonPath("$.data[*].id", Matchers.contains(content1.getId().intValue(), content2.getId().intValue(), content3.getId().intValue())));

    }

    @Test
    @DisplayName("랭킹 조회 성공 빈리스트")
    void 랭킹_조회_성공_빈리스트 () throws Exception {

        int rankingNum = 10;

        //  given
        given(rankingService.readDailyRankingContents(rankingNum)).willReturn(List.of());

        //  when & then
        mockMvc.perform(get("/api/v1/ranking/top/"+rankingNum)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(0)));

    }

    @Test
    @DisplayName("랭킹 조회 - 100개 이상 요청 시 100개로 제한되어 서비스가 호출 된다")
    void 최대_조회_개수_제한_검증() throws Exception {

        // 테스트 데이터 세팅
        int maxRankingNum = 100;
        int excessiveNum = 999;

        //  given
        given(rankingService.readDailyRankingContents(maxRankingNum)).willReturn(List.of());

        //  when
        mockMvc.perform(get("/api/v1/ranking/top/"+excessiveNum)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        //  then
        verify(rankingService, times(1)).readDailyRankingContents(maxRankingNum);

    }

}
