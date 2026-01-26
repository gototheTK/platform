package app.project.platform.service;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentImage;
import app.project.platform.entity.ContentLike;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.handler.FileHandler;
import app.project.platform.repository.ContentImageRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class ContentServiceTest {

    @InjectMocks
    ContentService contentService;

    @Mock
    ContentRepository contentRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    ContentImageRepository contentImageRepository;

    @Mock
    ContentLikeRepository contentLikeRepository;

    @Mock
    FileHandler fileHandler;

    @Test
    void 게시글_작성() throws IOException {

        String writerEmail = "writer@emai.com";
        String writerNickname = "writer";

        // given
        ContentCreateRequestDto contentCreateRequestDto = ContentCreateRequestDto.builder()
                .title("test_title")
                .description("test_description")
                .category(ContentCategory.CARTOON.getName())
                .build();

        List<MultipartFile> files = List.of(mock(MultipartFile.class), mock(MultipartFile.class), mock(MultipartFile.class));

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email(writerEmail)
                .role(Role.USER.getName())
                .nickname(writerNickname)
                .build();

        Member member = Member.builder()
                        .email(writerEmail)
                                .password("password")
                                        .role(Role.USER)
                                                .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        Content content = Content.builder()
                .author(member)
                .title("test_title")
                .description("test_description")
                .category(ContentCategory.from(contentCreateRequestDto.getCategory()))
                .build();

        ReflectionTestUtils.setField(content, "id", 1L);

        given(contentRepository.save(any())).willReturn(content);

        ContentImage contentImage = ContentImage.builder()
                .content(content)
                .originalFileName("test.jpg")
                .storeFilename(UUID.randomUUID() + "_" + "test.jpg")
                .build();

        ReflectionTestUtils.setField(contentImage, "id", 1L);

        given(fileHandler.storeFile(any(), any())).willReturn(contentImage);
        given(contentImageRepository.save(contentImage)).willReturn(contentImage);

        //  when
        Long savedId = contentService.create(contentCreateRequestDto, files, memberDto);

        //  then
        //  1. 캡쳐(납치) 도구 준비
        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);

        //  2. verify하면서 동시에 '납치(capture)' 수행
        verify(contentRepository, times(1)).save(captor.capture());

        // ----------------------------------------------------
        // 👇 [추가해야 할 부분] 납치한 녀석을 꺼내서 취조해야 합니다!
        // ----------------------------------------------------
        Content capturedContent = captor.getValue();    //  범인 확보

        //  3. 검증: "서비스가 만든 객체의 내용이 내 요청이랑 똑같아?"
        assertThat(capturedContent.getTitle()).isEqualTo("test_title");
        assertThat(capturedContent.getAuthor().getEmail()).isEqualTo(writerEmail);
        assertThat(capturedContent.getCategory()).isEqualTo(ContentCategory.CARTOON);

        verify(memberRepository, times(1)).findById(memberDto.getId());
        verify(fileHandler, times(files.size())).storeFile(any(), any());
        verify(contentImageRepository, times(files.size())).save(any());

        assertThat(savedId).isEqualTo(content.getId());

    }

    @Test
    void 게시글_수정_권한_검사 () {

        Long id = 1L;

        ContentUpdateRequestDto contentRequestDto = ContentUpdateRequestDto.builder()
                .id(id)
                .category(ContentCategory.NOVEL.getName())
                .title("test_title")
                .description("test_description")
                .build();

        MemberDto memberDto = MemberDto.builder()
                .id(2L)
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER.getName())
                .build();

        Member member = Member.builder()
                .email("test@email.com")
                .nickname("test")
                .role(Role.USER)
                .build();

        ReflectionTestUtils.setField(member, "id", 1L);

        Content content = Content.builder()
                .title("test_title")
                .description("test_description")
                .author(member)
                .build();

        List<MultipartFile> files = null;

        given(contentRepository.findById(id)).willReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.update(id, contentRequestDto, files, memberDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());

    }

    @Test
    @DisplayName("좋아요 성공 검사")
    void 좋아요_성공() {

        // 글
        Long contentId = 1L;

        // 회원
        MemberDto memberDto = MemberDto.builder().id(1L).build();

        Content content = Content.builder().build();
        ReflectionTestUtils.setField(content, "id", 1L);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", 1L);

        ContentLike contentLike = ContentLike.builder()
                .content(content)
                .member(member)
                .build();

        ReflectionTestUtils.setField(contentLike, "id", 1L);

        // given
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));
        given(contentLikeRepository.existsByContentAndMember(content, member)).willReturn(false);
        given(contentLikeRepository.save(any())).willReturn(contentLike);

        //  when
        Long likeId = contentService.addLike(contentId, memberDto);

        //  then
        ArgumentCaptor<ContentLike> captor = ArgumentCaptor.forClass(ContentLike.class);

        then(contentRepository).should(times(1)).findById(contentId);
        then(memberRepository).should(times(1)).findById(memberDto.getId());
        // 여기서 any()가 아니어도 상관없나?
        then(contentLikeRepository).should(times(1)).existsByContentAndMember(content, member);
        then(contentLikeRepository).should(times(1)).save(captor.capture());

        ContentLike capturedContentLike = captor.getValue();

        // contentLike가 제대로 만들어 졌는지 확인
        assertThat(capturedContentLike.getContent().getId()).isEqualTo(contentId);
        assertThat(capturedContentLike.getMember().getId()).isEqualTo(memberDto.getId());

        //  sava 결과 확인
        assertThat(likeId).isEqualTo(contentLike.getId());

    }


}
