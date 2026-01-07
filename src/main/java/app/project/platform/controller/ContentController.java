package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.ModifyRequestDto;
import app.project.platform.domain.dto.WriteRequestDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.exception.BusinessException;
import app.project.platform.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/write")
    public ResponseEntity<ApiResponse<Long>> write(
            @RequestBody @Valid WriteRequestDto writeRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) MemberDto memberDto
    ) {

        if (memberDto == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Long contentId = contentService.write(memberDto, writeRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(contentId));
    }

    @PostMapping("/modify")
    public ResponseEntity<ApiResponse<Long>> update(
            @RequestBody @Valid ModifyRequestDto modifyRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) MemberDto memberDto
    ) {

        if (memberDto == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Long contentId = contentService.modify(memberDto, modifyRequestDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(contentId));
    }

}
