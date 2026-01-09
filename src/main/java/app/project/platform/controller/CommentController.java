package app.project.platform.controller;

import app.project.platform.domain.ApiResponse;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponseDto>> create(
            @RequestBody @Valid CommentRequestDto commentRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) Member member
    ) {

        if (member == null) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        CommentResponseDto commentResponseDto = commentService.create(member, commentRequestDto);

        return ResponseEntity
                .ok(ApiResponse.success(commentResponseDto));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> update(
            @PathVariable(name = "id") Long id,
            @RequestBody @Valid CommentRequestDto commentRequestDto,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) Member member
    ) {

        if (member == null) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        CommentResponseDto commentResponseDto = commentService.update(member, commentRequestDto);

        return ResponseEntity
                .ok(ApiResponse.success(commentResponseDto));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponseDto>> delete(
            @PathVariable(name = "id") Long id,
            @SessionAttribute(name = "LOGIN_MEMBER", required = false) Member member
    ) {

        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        commentService.delete(member, id);

        return ResponseEntity
                .ok(ApiResponse.success(null));
    }

}
