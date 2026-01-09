package app.project.platform.service;

import app.project.platform.domain.dto.ContentDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.ModifyRequestDto;
import app.project.platform.domain.dto.WriteRequestDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.entity.Category;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.CategoryRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final CategoryRepository categoryRepository;

    private final ContentRepository contentRepository;

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<ContentDto> list(Pageable pageable) {
        Page<Content> contents = contentRepository.findAll(pageable);

        return contents.map(ContentDto::from);
    }

    @Transactional(readOnly = true)
    public ContentDto read(Long id) {

        return ContentDto.from(contentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND)));
    }

    @Transactional
    public Long create(MemberDto memberDto, WriteRequestDto writeRequestDto) {

        Category category = categoryRepository.findById(writeRequestDto.getCategoryId()).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Member author = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Content content = Content.builder()
                .title(writeRequestDto.getTitle())
                .description(writeRequestDto.getDescription())
                .author(author)
                .category(category)
                .build();

        return ContentDto.from(contentRepository.save(content)).getId();

    }

    @Transactional
    public Long modify(MemberDto memberDto, ModifyRequestDto modifyRequestDto) {

        Content content = contentRepository.findById(modifyRequestDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if(!memberDto.getId().equals(content.getAuthor().getId())) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        content.update(modifyRequestDto.getTitle(), modifyRequestDto.getDescription(), modifyRequestDto.getCategory());

        return content.getId();

    }

    @Transactional
    public void delete(Long id) {
        contentRepository.deleteById(id);
    }

}
