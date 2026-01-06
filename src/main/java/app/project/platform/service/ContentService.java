package app.project.platform.service;

import app.project.platform.dto.ContentDTO;
import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;

    public Page<Content> getList(ContentDTO contentDTO) {

        // 1. 정렬 기준 설정 (작성일시 createdDate 내림차순 DESC)
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createdDate"));

        // 2. Pageable 객체 생성
        // page: 조회할 페이지 번호 (0부터 시작)
        // 10: 한 페이지에 보여줄 게시물 개수
        Pageable pageable = PageRequest.of(contentDTO.getPage(), 10, Sort.by(sorts));

        // 3. 페이징 처리된 결과 반환
        return contentRepository.findAll(pageable);

    }

}
