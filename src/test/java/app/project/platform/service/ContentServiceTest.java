package app.project.platform.service;

import app.project.platform.domain.PostRedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Content;
import app.project.platform.entity.ContentImage;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.handler.FileHandler;
import app.project.platform.repository.ContentImageRepository;
import app.project.platform.repository.ContentLikeRepository;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    ListOperations<String, Object> listOperations;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    SetOperations<String, Object> setOperations;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    @Mock
    HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        //  RedisTemplate의 opsFor 메서드들이 호출 될 때 Mock 객체를 반환하도록 설정
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void 게시글가져오기_피드버퍼가_없을_때() {

        //  given
        Long memberId = 1L;
        MemberDto memberDto = MemberDto.builder()
                .id(memberId)
                .nickname("testUser")
                .build();
        PageRequest pageable = PageRequest.of(0, 10);

        //  1. 피드 버퍼가 비어있다고 가정
        given(listOperations.range(anyString(), anyLong(), anyLong())).willReturn(new ArrayList<>());

        //  2. 커서 값 가정 (가장 최신)
        given(valueOperations.get(anyString())).willReturn(null);

        //  3. DB에서 반환될 임시 게시글 50개(여기서는 테스트를 위해 15개만) 생성
        List<Content> mockContents = createMockContents(50);
        given(contentRepository.findAllWithAuthorByCursor(anyLong(), eq(pageable))).willReturn(mockContents);

        //  4. 유저의 취향 벡터 (Mocking)
        Map<Object, Object> mockViewVector = new HashMap<>();
        mockViewVector.put(ContentCategory.CARTOON.name(), "10");
        String viewRedisKey = PostRedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(memberId);
        given(hashOperations.entries(eq(viewRedisKey))).willReturn(mockViewVector);

        Map<Object, Object> mockLikeVector = new HashMap<>();
        mockLikeVector.put(ContentCategory.CARTOON.name(), "5");
        String likeRedisKey = PostRedisKey.MEMBER_CATEGORY_VIEW_COUNT.makeKey(memberId);
        given(hashOperations.entries(eq(likeRedisKey))).willReturn(mockLikeVector);

        //  when
        Slice<ContentResponseDto> result = contentService.list(pageable, memberDto);

        //  then
        //  1. 정상적으로 10개의 데이터가 반환되었는가?
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.hasNext()).isTrue();

        // 2. 커서가 잘 갱신되었는가? (마지막 게시글 ID로 set 되었는지 검증)
        String feedCursorKey = PostRedisKey.FEED_CURSOR.makeKey(memberId);
        verify(valueOperations, times(1)).set(eq(feedCursorKey), eq(String.valueOf(mockContents.get(49).getId())));

        // 3. 남은 5개의 글이 Redis 버퍼에 잘 Push 되었는가?
        String bufferCursorKey= PostRedisKey.FEED_BUFFER.makeKey(memberId);
        verify(listOperations, times(1)).rightPushAll(eq(bufferCursorKey), any(Object[].class));

        // 4. (중요) TECH 카테고리 글이 상위로 정렬되었는지 확인하는 로직 추가 가능
        boolean isTrue = result.getContent().stream().allMatch(dto -> dto.getCategory().equals(ContentCategory.CARTOON.getName()));
        assertThat(isTrue).isTrue();

    }

    //  테스트용 데이터 생성 유틸
    private List<Content> createMockContents(int count) {
        List<Content> contents = new ArrayList<>();

        for (long i = 1; i <= count; i++) {

            Member member = Member.builder().build();
            ReflectionTestUtils.setField(member, "id", i);

            Content content = Content.builder()
                    .title("제목 " + i)
                    .category(i % 2 == 0 ?ContentCategory.CARTOON : ContentCategory.NOVEL)
                    .author(member)
                    .build();
            contents.add(content);
        }

        return contents;
    }

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
        given(redisTemplate.opsForSet()).willReturn(setOperations);

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
    void 좋아요_성공_캐시_히트() {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;
        Long totalCount = 15L;
        double delta = 1;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);
        String redisLikeContentUsersQueue = PostRedisKey.LIKE_CONTENT_USERS_QUEUE.makeKey(contentId);
        String redisLikeContentCount = PostRedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        String redisLikeDailyRankingCount = PostRedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today);
        String redisLikeUpdatedContents = PostRedisKey.LIKE_UPDATED_CONTENTS.makeKey();


        //  given (Mocking)

        // Redis 처리 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        
        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(true);
        given(setOperations.add(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(1L);
        given(valueOperations.increment(eq(redisLikeContentCount))).willReturn(totalCount);

        //  when
        Long resultId = contentService.addLike(contentId, memberDto);
        assertThat(resultId).isEqualTo(totalCount);

        // when
        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).add(eq(redisLikeContentUsers), eq(memberDto.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeContentUsersQueue), eq(memberDto.getId()));
        verify(valueOperations, times(1)).increment(eq(redisLikeContentCount));
        verify(zSetOperations, times(1)).incrementScore(eq(redisLikeDailyRankingCount), eq(contentId), eq(delta));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedContents), eq(contentId));
        
    }

    @Test
    @DisplayName("좋아요 성공 검사")
    void 좋아요_성공_캐시_미스_DB_조회() {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;
        Long totalCount = 15L;
        long success = 1L;
        double delta = 1;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);
        String redisLikeContentUsersQueue = PostRedisKey.LIKE_CONTENT_USERS_QUEUE.makeKey(contentId);
        String redisLikeContentCount = PostRedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        String redisLikeDailyRankingCount = PostRedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today);
        String redisLikeUpdatedContents = PostRedisKey.LIKE_UPDATED_CONTENTS.makeKey();


        //  given
        // Redis 처리 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(false);
        given(contentRepository.existsById(eq(contentId))).willReturn(true);
        given(setOperations.add(eq(redisValidContents), eq(contentId))).willReturn(1L);
        given(setOperations.add(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(success);
        given(valueOperations.increment(eq(redisLikeContentCount))).willReturn(totalCount);

        //  when
        Long resultId = contentService.addLike(contentId, memberDto);
        assertThat(resultId).isEqualTo(totalCount);

        // when
        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(contentRepository, times(1)).existsById(eq(contentId));
        verify(setOperations, times(1)).add(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).add(eq(redisLikeContentUsers), eq(memberDto.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeContentUsersQueue), eq(memberDto.getId()));
        verify(valueOperations, times(1)).increment(eq(redisLikeContentCount));
        verify(zSetOperations, times(1)).incrementScore(eq(redisLikeDailyRankingCount), eq(contentId), eq(delta));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedContents), eq(contentId));

    }

    @Test
    @DisplayName("좋아요_실패_글_미존재")
    void 좋아요_실패_글_미존재 () {
        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        // Redis 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();

        //  given
        //  Reids 처리 반환
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        //  로직 처리 반환
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(false);
        given(contentRepository.existsById(eq(contentId))).willReturn(false);

        //  when & then
        assertThatThrownBy(()->contentService.addLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e->((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);

        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(contentRepository, times(1)).existsById(eq(contentId));

    }

    @Test
    @DisplayName("좋아요_실패_좋아요_중복")
    void 좋아요_실패_좋아요_중복 () {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberDto.getId());

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);

        //  given (Mocking)

        // Redis 처리 반환
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(true);
        given(setOperations.add(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(0L);

        // when & then
        assertThatThrownBy(()->contentService.addLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e->((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_LIKED);

        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).add(eq(redisLikeContentUsers), eq(memberDto.getId()));

    }

    @Test
    @DisplayName("좋아요_취소_성공 캐시 히트")
    void 좋아요_취소_성공_캐시_히트 () {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;
        double delta = -1;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);
        String redisLikeContentUsersRemoveQueue = PostRedisKey.LIKE_CONTENT_USERS_REMOVE_QUEUE.makeKey(contentId);
        String redisLikeContentCount = PostRedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        String redisLikeDailyRankingCount = PostRedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today);
        String redisLikeUpdatedContents = PostRedisKey.LIKE_UPDATED_CONTENTS.makeKey();


        //  given (Mocking)

        // Redis 처리 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(true);
        given(setOperations.remove(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(1L);

        //  when
        contentService.removeLike(contentId, memberDto);

        // when
        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).remove(eq(redisLikeContentUsers), eq(memberDto.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeContentUsersRemoveQueue), eq(memberDto.getId()));
        verify(valueOperations, times(1)).decrement(eq(redisLikeContentCount));
        verify(zSetOperations, times(1)).incrementScore(eq(redisLikeDailyRankingCount), eq(contentId), eq(delta));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedContents), eq(contentId));

    }

    @Test
    @DisplayName("좋아요_취소_성공 캐시 미스 DB 조회")
    void 좋아요_취소_성공_캐시_미스_DB_조회 () {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;
        double delta = -1;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);
        String redisLikeContentUsersRemoveQueue = PostRedisKey.LIKE_CONTENT_USERS_REMOVE_QUEUE.makeKey(contentId);
        String redisLikeContentCount = PostRedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        String redisLikeDailyRankingCount = PostRedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today);
        String redisLikeUpdatedContents = PostRedisKey.LIKE_UPDATED_CONTENTS.makeKey();


        //  given (Mocking)

        // Redis 처리 반환
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(redisTemplate.opsForList()).willReturn(listOperations);

        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(false);
        given(contentRepository.existsById(eq(contentId))).willReturn(true);
        given(setOperations.remove(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(1L);

        //  when
        contentService.removeLike(contentId, memberDto);

        // when
        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(contentRepository, times(1)).existsById(eq(contentId));
        verify(setOperations, times(1)).add(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).remove(eq(redisLikeContentUsers), eq(memberDto.getId()));
        verify(listOperations, times(1)).rightPush(eq(redisLikeContentUsersRemoveQueue), eq(memberDto.getId()));
        verify(valueOperations, times(1)).decrement(eq(redisLikeContentCount));
        verify(zSetOperations, times(1)).incrementScore(eq(redisLikeDailyRankingCount), eq(contentId), eq(delta));
        verify(setOperations, times(1)).add(eq(redisLikeUpdatedContents), eq(contentId));

    }

    @Test
    @DisplayName("좋아요_취소_실패_글_미존재")
    void 좋아요_취소_실패_글_미존재 () {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        // Redis 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();

        //  given
        //  Reids 처리 반환
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        //  로직 처리 반환
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(false);
        given(contentRepository.existsById(eq(contentId))).willReturn(false);

        //  when & then
        assertThatThrownBy(()->contentService.removeLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e->((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);

        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(contentRepository, times(1)).existsById(eq(contentId));

    }

    @Test
    @DisplayName("좋아요_실패_좋아요_미존재")
    void 좋아요_실패_좋아요_미존재 () {

        // 파라미터 및 테스트 데이터 세팅
        Long contentId = 100L; // (구분을 위해 1L 대신 100L 사용)
        Long memberId = 1L;

        MemberDto memberDto = MemberDto.builder().id(memberId).build();

        // Redis의 키
        String redisValidContents = PostRedisKey.VALID_CONTENTS.makeKey();
        String redisLikeContentUsers = PostRedisKey.LIKE_CONTENT_USERS.makeKey(contentId);

        //  given (Mocking)

        // Redis 처리 반환
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 로직 반환 처리
        given(setOperations.isMember(eq(redisValidContents), eq(contentId))).willReturn(true);
        given(setOperations.remove(eq(redisLikeContentUsers), eq(memberDto.getId()))).willReturn(0L);

        // when & then
        assertThatThrownBy(()->contentService.removeLike(contentId, memberDto))
                .isInstanceOf(BusinessException.class)
                .extracting(e->((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIKE_NOT_FOUND);

        verify(setOperations, times(1)).isMember(eq(redisValidContents), eq(contentId));
        verify(setOperations, times(1)).remove(eq(redisLikeContentUsers), eq(memberDto.getId()));

    }

}
