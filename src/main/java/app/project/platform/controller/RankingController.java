package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/top/{num}")
    public ResponseEntity<ApiResponse<List<ContentResponseDto>>> readRakingTop (@PathVariable("num") int num) {

        if (num > 100) num = 100;

        List<ContentResponseDto> list = rankingService.readDailyRankingContents(num);

        return ResponseEntity.ok(ApiResponse.success(list));

    }

}
