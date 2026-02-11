package app.project.platform.service;

import app.project.platform.domain.RedisKey;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


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

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    SetOperations<String, Object> setOperations;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

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

        given(contentRepository.findByIdWithAuthor(id)).willReturn(Optional.of(content));

        assertThatThrownBy(() -> contentService.update(id, contentRequestDto, files, memberDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());

    }

    @Test
    @DisplayName("좋아요 성공 검사")
    void 좋아요_성공() {

        // 1. 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;
        Long contentLikeId = 55L;

        //  상수 (서비스 코드와 동일하게 맞춤)
        String LIKE_CONTENT_USERS = RedisKey.LIKE_CONTENT_USERS.getPrefix();
        String LIKE_CONTENT_COUNT = RedisKey.LIKE_CONTENT_COUNT.getPrefix();
        String LIKE_DAILY_RANKING_COUNT = RedisKey.LIKE_DAILY_RANKING_COUNT.getPrefix() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String LIKE_UPDATED_CONTENTS = RedisKey.LIKE_UPDATED_CONTENTS.getPrefix();

        // 회원 및 글
        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        Content content = Content.builder().build();
        ReflectionTestUtils.setField(content, "id", contentId);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        //  given (Mocking)
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        //  Key 생성 로직을 서비스와 일치시킴 (ContentId 사용)
        String expectedUserKey = LIKE_CONTENT_USERS + contentId;
        given(setOperations.add(eq(expectedUserKey), eq(memberId))).willReturn(1L);

        //  DGB 저장
        given(contentLikeRepository.save(any())).willAnswer( invocationOnMock -> {

            ContentLike contentLike  = invocationOnMock.getArgument(0);
            ReflectionTestUtils.setField(contentLike, "id", contentLikeId);
            return contentLike;
        });

        //  when
        Long resultId = contentService.addLike(contentId, memberDto);

        //  then

        //  1. 리턴값 검증
        assertThat(resultId).isEqualTo(contentLikeId);

        //  2. JPA 조회 검증
        verify(contentRepository, times(1)).findById(contentId);
        verify(memberRepository, times(1)).findById(memberId);

        //  3.  Redis Set 중복 체크 검증 (Key 확인)
        verify(setOperations, times(1)).add(eq(expectedUserKey), eq(member.getId()));

        //  4. DB 저장 검증
        ArgumentCaptor<ContentLike> contentLikeArgumentCaptor = ArgumentCaptor.forClass(ContentLike.class);
        verify(contentLikeRepository, times(1)).save(contentLikeArgumentCaptor.capture());

        assertThat(contentLikeArgumentCaptor.getValue().getContent().getId()).isEqualTo(contentId);
        assertThat(contentLikeArgumentCaptor.getValue().getMember().getId()).isEqualTo(memberId);

        //  5. Redis 카운트 검증
        String expectedCountKey = LIKE_CONTENT_COUNT + contentId;
        ArgumentCaptor<String> countKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(1)).increment(countKeyArgumentCaptor.capture());
        assertThat(countKeyArgumentCaptor.getValue()).isEqualTo(expectedCountKey);
        verify(zSetOperations, times(1)).incrementScore(LIKE_DAILY_RANKING_COUNT, contentId, 1);
        verify(setOperations, times(1)).add(LIKE_UPDATED_CONTENTS, contentId);
    }

    @Test
    @DisplayName("좋아요_실패_중복")
    void 좋아요_실패_중복 () {

        //  테스트 데이터 세팅
        Long contentId = 100L;
        Long memberId = 1L;

        // Redis 상수
        String LIKE_CONTENT_USERS = RedisKey.LIKE_CONTENT_USERS.getPrefix();

        //  회원 및 글 객체 생성
        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        Content content = Content.builder().build();
        ReflectionTestUtils.setField(content, "id", contentId);

        //  given
        //  회원 및 글
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        //  좋아요 사용
        String expectedUserLikeKey = LIKE_CONTENT_USERS + contentId;
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.add(expectedUserLikeKey, member.getId())).willReturn(0L);

        //  when
        assertThatThrownBy(() -> contentService.addLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_LIKED);

        //  then
        verify(contentRepository, times(1)).findById(contentId);
        verify(memberRepository, times(1)).findById(memberId);
        verify(contentLikeRepository, times(0)).save(any());
        verify(valueOperations, times(0)).increment(any());
        verify(setOperations, times(1)).add(any(), any());
        verify(zSetOperations, times(0)).incrementScore(any(), any(), eq(1));

    }

    @Test
    @DisplayName("좋아요_취소_성공")
    void 좋아요_취소_성공 () {

        //  테스트 데이터 세팅
        Long contentId = 100L;
        Long memberId = 1L;

        MemberDto memberDto = MemberDto.builder().build();
        ReflectionTestUtils.setField(memberDto, "id", memberId);

        //  상수
        String LIKE_CONTENT_USERS = RedisKey.LIKE_CONTENT_USERS.getPrefix();
        String LIKE_CONTENT_COUNT = RedisKey.LIKE_CONTENT_COUNT.getPrefix();
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String LIKE_DAILY_RANKING_COUNT = RedisKey.LIKE_DAILY_RANKING_COUNT.getPrefix() + today;
        String LIKE_UPDATED_CONTENTS = RedisKey.LIKE_UPDATED_CONTENTS.getPrefix();

        //  회원 및 글 객체, Redis 키 생성
        Content content = Content.builder().build();
        ReflectionTestUtils.setField(content, "id", contentId);

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);

        String userLikeKey = LIKE_CONTENT_USERS + contentId;

        //  given
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.remove(userLikeKey, member.getId())).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        //  when
        contentService.removeLike(contentId, memberDto);

        //  then
        
        // 글, 회원, Redis 값 불러오기
        verify(contentRepository, times(1)).findById(contentId);
        verify(memberRepository, times(1)).findById(memberId);
        verify(setOperations, times(1)).remove(userLikeKey, member.getId());

        // DB 저장
        ArgumentCaptor<Content> contentArgumentCaptor = ArgumentCaptor.forClass(Content.class);
        ArgumentCaptor<Member> memberArgumentCaptor = ArgumentCaptor.forClass(Member.class);

        verify(contentLikeRepository, times(1)).deleteByContentAndMember(contentArgumentCaptor.capture(), memberArgumentCaptor.capture());

        Content capturedContent = contentArgumentCaptor.getValue();
        assertThat(capturedContent.getId()).isEqualTo(contentId);

        Member capturedMember = memberArgumentCaptor.getValue();
        assertThat(capturedMember.getId()).isEqualTo(memberId);

        //  Redis 카운트 감소
        ArgumentCaptor<String> countKeyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        String countKey = LIKE_CONTENT_COUNT + contentId;
        verify(valueOperations, times(1)).decrement(countKeyArgumentCaptor.capture());
        assertThat(countKeyArgumentCaptor.getValue()).isEqualTo(countKey);
        verify(zSetOperations, times(1)).incrementScore(LIKE_DAILY_RANKING_COUNT, contentId, -1);
        verify(setOperations, times(1)).add(LIKE_UPDATED_CONTENTS, contentId);

    }

    @Test
    @DisplayName("좋아요_취소_실패_미존재")
    void 좋아요_취소_실패_미존재 () {

        //  테스트 데이터 세팅
        Long contentId = 100L;
        Long memberId = 1L;

        // 상수
        String LIKE_CONTENT_USERS = RedisKey.LIKE_CONTENT_USERS.getPrefix();

        //  회원 및 글 객체 세팅
        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        Content content = Content.builder().build();
        ReflectionTestUtils.setField(content, "id", contentId);

        //  given
        //  글 및 회원 조회
        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
        given(memberRepository.findById(memberDto.getId())).willReturn(Optional.of(member));

        //  redis 조회
        String userLikeKey = LIKE_CONTENT_USERS + contentId;
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.remove(userLikeKey, member.getId())).willReturn(0L);

        //  when & then
        assertThatThrownBy(() -> contentService.removeLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIKE_NOT_FOUND);

        //  처리 되는것
        verify(contentRepository, times(1)).findById(contentId);
        verify(memberRepository, times(1)).findById(memberId);
        verify(setOperations).remove(userLikeKey, member.getId());

        //  처리 안되는것
        verify(contentLikeRepository, times(0)).deleteByContentAndMember(any(), any());
        verify(valueOperations, times(0)).decrement(any());
        verify(zSetOperations, times(0)).incrementScore(any(), any(), eq(-1));
        verify(setOperations, times(0)).add(any(), any());

    }

}
