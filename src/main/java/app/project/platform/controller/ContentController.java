package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContentResponseDto>>> list(
            @PageableDefault(size = 10, page = 0, direction = Sort.Direction.DESC, sort = "id") Pageable pageable) {

        Page<ContentResponseDto> list = contentService.list(pageable);

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentResponseDto>> read(
            @PathVariable(name = "id") Long id) {

        ContentResponseDto contentResponseDto = contentService.read(id);

        return ResponseEntity.ok(ApiResponse.success(contentResponseDto));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> create(
            @RequestPart(name = "request") @Valid ContentCreateRequestDto contentCreateRequestDto,
            @RequestPart(name = "files") List<MultipartFile> files,
            @SessionAttribute(name = "LOGIN_MEMBER") MemberDto memberDto) throws IOException {

        Long contentId = contentService.create(contentCreateRequestDto, files, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContentResponseDto>> update(
            @PathVariable(name = "id") Long id,
            @RequestPart(name = "request") @Valid ContentUpdateRequestDto contentUpdateRequestDto,
            @RequestPart(name = "files") List<MultipartFile> files,
            @SessionAttribute(name = "LOGIN_MEMBER") MemberDto memberDto) throws IOException {

        ContentResponseDto contentResponseDto = contentService.update(id, contentUpdateRequestDto, files, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentResponseDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable(name = "id") Long id,
            @SessionAttribute(name = "LOGIN_MEMBER") MemberDto memberDto) {

        contentService.delete(id, memberDto);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("{id}/like")
    public ResponseEntity<ApiResponse<Long>> addLike(
            @PathVariable(name = "id") Long id,
            @SessionAttribute(name = "LOGIN_MEMBER") MemberDto memberDto) {

        Long contentLikeId = contentService.addLike(id, memberDto);

        return ResponseEntity.ok(ApiResponse.success(contentLikeId));
    }

    @DeleteMapping("{id}/like")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @PathVariable(name = "id") Long id,
            @SessionAttribute(name = "LOGIN_MEMBER") MemberDto memberDto) {

        contentService.removeLike(id ,memberDto);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
