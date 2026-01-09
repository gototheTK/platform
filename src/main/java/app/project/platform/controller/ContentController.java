package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.ContentDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.ModifyRequestDto;
import app.project.platform.domain.dto.WriteRequestDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.exception.BusinessException;
import app.project.platform.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContentDto>>> list(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)Pageable pageable
    ) {

        Page<ContentDto> contents = contentService.list(pageable);

        return ResponseEntity
                .ok(ApiResponse.success(contents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentDto>> read (
            @PathVariable(name = "id", required = true) Long id
    ) {

        ContentDto content = contentService.read(id);

        return ResponseEntity
                .ok(ApiResponse.success(content));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @RequestBody @Valid WriteRequestDto writeRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) MemberDto memberDto
    ) {

        if (memberDto == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Long contentId = contentService.create(memberDto, writeRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(contentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Long>> update(
            @PathVariable(name = "id") Long id,
            @RequestBody @Valid ModifyRequestDto modifyRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) MemberDto memberDto
    ) {

        if (memberDto == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Long contentId = contentService.modify(memberDto, modifyRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(contentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete (
            @PathVariable(name = "id", required = true) Long id
    ) {
        contentService.delete(id);

        return ResponseEntity
                .ok(ApiResponse.success(null));
    }

}
