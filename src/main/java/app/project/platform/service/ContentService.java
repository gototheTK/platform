package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentImage;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.handler.FileHandler;
import app.project.platform.repository.ContentImageRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final FileHandler fileHandler;

    private final MemberRepository memberRepository;

    private final ContentRepository contentRepository;

    private final ContentImageRepository contentImageRepository;

    // JPA가 영속성컨텍스트에서 스내샷을 만들어서, 변경감지를하지 않게합니다.
    // 그럼으로써 객체나 메모리 낭비를 하지 않게 합니다.
    @Transactional(readOnly = true)
    public Page<ContentResponseDto> list (Pageable pageable) {
        return contentRepository.findAll(pageable)
                .map(ContentResponseDto::of);
    }

    @Transactional(readOnly = true)
    public ContentResponseDto read (Long id) {

        Content content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        return ContentResponseDto.of(content);
    }

    // 스프링의 @Transactional은 기본적으로 Unchecked Exception(RuntimeException)만 롤백한다.
    // Checked Exception을 롤백하기 위해서는 rollbackFor옵션을 지정하여 주어야한다.
    @Transactional(rollbackFor = Exception.class)
    public Long create (ContentCreateRequestDto contentCreateRequestDto, List<MultipartFile> files, MemberDto memberDto) throws IOException {

        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 1. 게시글 저장
        Content content = Content.builder()
                .title(contentCreateRequestDto.getTitle())
                .description(contentCreateRequestDto.getDescription())
                .category(ContentCategory.from(contentCreateRequestDto.getCategory()))
                .author(member)
                .build();

        Content savedContent = contentRepository.save(content);

        // 2. 파일 저장
        if (!files.isEmpty()) {
            for (MultipartFile file : files) {
                // FileHandler가 실제 파일 저장 후 엔티티 리턴
                ContentImage image = fileHandler.storeFile(file, content);
                contentImageRepository.save(image); // DB에 정보 저장
            }
        }

        return savedContent.getId();
    }

    // 스프링의 @Transactional은 기본적으로 Unchecked Exception(Runtime Exception)만 롤백한다.
    // Checked Exception 예외 발생 시 롤백을 하기 위해서는, rollbackFor옵션을 지정하여 주어야 한다.
    @Transactional(rollbackFor = Exception.class)
    public ContentResponseDto update (Long id, ContentUpdateRequestDto contentUpdateRequestDto, List<MultipartFile> files, MemberDto memberDto) throws IOException {

        Content content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!content.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<ContentImage> images = content.getFiles();
        
        // 1. 디스크에서 파일 먼저 다 지우기
        for (ContentImage file : content.getFiles()) {
            fileHandler.deleteFile(file.getStoreFilename());
        }

        // 리스트 비우기
        images.clear();

        for (MultipartFile file : files) {
            ContentImage contentImage = fileHandler.storeFile(file, content);
            contentImageRepository.save(contentImage);
            content.getFiles().add(contentImage);
        }

        content.update(contentUpdateRequestDto.getTitle(), contentUpdateRequestDto.getDescription(), ContentCategory.from(contentUpdateRequestDto.getCategory()));

        return ContentResponseDto.of(content);

    }

    @Transactional
    public void delete (Long id, MemberDto memberDto) {

        Content content = contentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!content.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<ContentImage> files = content.getFiles();

        for (ContentImage file : files) {
            fileHandler.deleteFile(file.getStoreFilename());
        }

        contentRepository.delete(content);

    }

}
