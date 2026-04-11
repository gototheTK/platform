package app.project.platform.controller;

import app.project.platform.annotation.LoginUser;
import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Slice<ContentResponseDto>>> list(
            @PageableDefault(size = 50, page = 0, sort="id", direction = Sort.Direction.DESC) Pageable pageable,
            @LoginUser MemberDto memberDto) {

        Slice<ContentResponseDto> list = contentService.list(pageable, memberDto);

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentResponseDto>> read(
            @PathVariable(name = "id") Long id,
            @LoginUser MemberDto memberDto) {

        ContentResponseDto contentResponseDto = contentService.read(id, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentResponseDto));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(
            @RequestPart(name = "request") @Valid ContentCreateRequestDto contentCreateRequestDto,
            @RequestPart(name = "files") List<MultipartFile> files,
            @LoginUser MemberDto memberDto) throws IOException {

        Long contentId = contentService.create(contentCreateRequestDto, files, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContentResponseDto>> update(
            @PathVariable(name = "id") Long id,
            @RequestPart(name = "request") @Valid ContentUpdateRequestDto contentUpdateRequestDto,
            @RequestPart(name = "files") List<MultipartFile> files,
            @LoginUser MemberDto memberDto) throws IOException {

        ContentResponseDto contentResponseDto = contentService.update(id, contentUpdateRequestDto, files, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentResponseDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable(name = "id") Long id,
            @LoginUser MemberDto memberDto) {

        contentService.delete(id, memberDto);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("{id}/like")
    public ResponseEntity<ApiResponse<Long>> addLike(
            @PathVariable(name = "id") Long id,
            @LoginUser MemberDto memberDto) {
        log.info("좋아요 요청 시도 - 게시글 ID: {}, 유저 ID: {}", id, memberDto.getId());
        Long contentLikeId = contentService.addLike(id, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentLikeId));
    }

    @DeleteMapping("{id}/like")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @PathVariable(name = "id") Long id,
            @LoginUser MemberDto memberDto) {

        log.info("좋아요 취소 요청 시도 - 게시글 ID: {}, 유저 ID: {}", id, memberDto.getId());
        contentService.removeLike(id ,memberDto);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
