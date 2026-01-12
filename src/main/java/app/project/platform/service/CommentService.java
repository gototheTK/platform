package app.project.platform.service;

import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.entity.Comment;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CommentRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final MemberRepository memberRepository;

    private final ContentRepository contentRepository;

    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponseDto create(MemberDto memberDto, CommentRequestDto commentRequestDto) {

        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

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
    public CommentResponseDto update(MemberDto memberDto, CommentRequestDto commentRequestDto) {

        Comment comment = commentRepository.findById(commentRequestDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(memberDto.getId())) throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);

        comment.update(commentRequestDto.getText());

        return CommentResponseDto.from(comment);

    }

    @Transactional
    public void delete(MemberDto memberDto, Long id) {

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthor().getId().equals(memberDto.getId())) throw new BusinessException(ErrorCode.COMMENT_WRITER_MISMATCH);

        commentRepository.delete(comment);
    }

}
