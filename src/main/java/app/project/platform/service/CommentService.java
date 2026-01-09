package app.project.platform.service;

import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final ContentRepository contentRepository;

    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponseDto create(Member member, CommentRequestDto commentRequestDto) {

        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Content content = contentRepository.findById(commentRequestDto.getContentId()).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        Comment parent = null;
        if (commentRequestDto.getParentId() != null) {

            parent = commentRepository.findById(commentRequestDto.getParentId()).orElseThrow(() -> new BusinessException(ErrorCode.CANNOT_REPLY_TO_DELETED));

            if (!parent.getContent().getId().equals(commentRequestDto.getContentId())) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);

        }

        Comment comment = Comment.builder()
                .content(content)
                .text(commentRequestDto.getText())
                .author(member)
                .parent(parent)
                .build();

        return CommentResponseDto.from(commentRepository.save(comment));

    }

    @Transactional
    public CommentResponseDto update(Member member, CommentRequestDto commentRequestDto) {

        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Comment comment = commentRepository.findById(commentRequestDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(member.getId())) throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);

        comment.update(commentRequestDto.getText());

        return CommentResponseDto.from(comment);

    }

    @Transactional
    public void delete(Member member, Long id) {

        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(member.getId())) throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);

        commentRepository.deleteById(id);
    }

}
