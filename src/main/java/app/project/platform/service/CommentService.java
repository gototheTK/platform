package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentLike;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ContentRepository contentRepository;

    private final MemberRepository memberRepository;

    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponseDto create(
            CommentRequestDto commentRequestDto,
            MemberDto memberDto) {

        Member member = memberRepository.findById(memberDto.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Content content = contentRepository.findById(commentRequestDto.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Comment parent = null;

        if (commentRequestDto.getParentId() != null) {
            parent = commentRepository.findById(commentRequestDto.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .text(commentRequestDto.getText())
                .author(member)
                .content(content)
                .parent(parent)
                .build();

        return CommentResponseDto.from(commentRepository.save(comment));

    }

    @Transactional
    public CommentResponseDto update(
            Long id,
            CommentRequestDto commentRequestDto,
            MemberDto memberDto) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        // 댓글 등록자가 댓글의 주인인가?
        if (!comment.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);
        }

        comment.update(commentRequestDto.getText());

        return CommentResponseDto.from(comment);
    }

    @Transactional
    public void delete (
            Long id,
            MemberDto memberDto) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH));

        // 댓글 등록자가 댓글의 주인인가?
        if (!comment.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);
        }

        commentRepository.delete(comment);
    }

}
