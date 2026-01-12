package app.project.platform.service;

import app.project.platform.domain.dto.ContentDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.ModifyRequestDto;
import app.project.platform.domain.dto.WriteRequestDto;
import app.project.platform.domain.type.ErrorCode;
import app.project.platform.entity.Category;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentImage;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.handler.FileHandler;
import app.project.platform.repository.CategoryRepository;
import app.project.platform.repository.ContentImageRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final FileHandler fileHandler;

    private final CategoryRepository categoryRepository;

    private final ContentRepository contentRepository;

    private final ContentImageRepository contentImageRepository;

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
    public Long create(MemberDto memberDto,
                       WriteRequestDto writeRequestDto,
                       MultipartFile thumbnail,
                       List<MultipartFile> images) {

        Category category = categoryRepository.findById(writeRequestDto.getCategoryId()).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Member author = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String thumbnailUrl = fileHandler.saveFile(thumbnail);

        Content content = Content.builder()
                .title(writeRequestDto.getTitle())
                .description(writeRequestDto.getDescription())
                .author(author)
                .category(category)
                .thumbnail_url(thumbnailUrl)
                .build();

        // 첨부 이미지들 저장
        if (images!=null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                String fileName = fileHandler.saveFile(file);
                if (fileName != null) {
                    ContentImage image = ContentImage.builder()
                            .content(content)
                            .fileUrl(fileName)
                            .originalUrl(file.getOriginalFilename())
                            .build();

                    contentImageRepository.save(image);
                }
            }
        }

        return content.getId();

    }

    @Transactional
    public Long update(MemberDto memberDto, ModifyRequestDto modifyRequestDto) {

        Content content = contentRepository.findById(modifyRequestDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if(!memberDto.getId().equals(content.getAuthor().getId())) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        content.update(modifyRequestDto.getTitle(), modifyRequestDto.getDescription(), modifyRequestDto.getCategory());

        return content.getId();

    }

    @Transactional
    public void delete(MemberDto memberDto, Long id) {

        Content content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!memberDto.getId().equals(content.getAuthor().getId())) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        // 1. 썸네일 이미지 삭제
        fileHandler.deleteFile(content.getThumbnail_url());

        // 2. 본문 첨부 이미지들 삭제
        for (ContentImage image : content.getContentImages()) {
            fileHandler.deleteFile(image.getFileUrl());
        }

        contentRepository.delete(content);
    }

}
