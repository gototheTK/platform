package app.project.platform.service;

import app.project.platform.domain.RedisKey;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.ContentUpdateRequestDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.ContentCategory;
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
import app.project.platform.util.SimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final FileHandler fileHandler;

    private final MemberRepository memberRepository;

    private final ContentRepository contentRepository;

    private final ContentImageRepository contentImageRepository;

    private final ContentLikeRepository contentLikeRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    // JPA가 영속성컨텍스트에서 스내샷을 만들어서, 변경감지를하지 않게합니다.
    // 그럼으로써 객체나 메모리 낭비를 하지 않게 합니다.
    @Transactional(readOnly = true)
    public Slice<ContentResponseDto> list (Pageable pageable, MemberDto memberDto) {

        int pageSize = 10;

        if (memberDto != null) {

            Long memberId = memberDto.getId();

            // 피드 버퍼에 글이 있는지 확인한다
            String feedBufferKey = RedisKey.FEED_BUFFER.makeKey(memberId);

            List<Object> cachedIds = redisTemplate.opsForList().range(feedBufferKey, 0, pageSize-1);

            // 피드 버퍼에 글이 존재한다면 리턴한다.
            if (cachedIds != null && !cachedIds.isEmpty()) {

                List<Long> idsToFetch = cachedIds.stream().map(o -> Long.valueOf(o.toString())).toList();

                //  2. JPQL 순서 분괴 해결: DB에서 퍼온 뒤 Java에서 순서 재조립
                List<Content> fetchedContents = contentRepository.findAllWithAuthorById(idsToFetch);
                Map<Long, Content> contentMap = fetchedContents.stream()
                        .collect(Collectors.toMap(Content::getId, c->c));

                List<ContentResponseDto> result = idsToFetch.stream()
                        .map(contentMap::get)
                        .filter(Objects::nonNull)
                        .map(ContentResponseDto::of)
                        .toList();

                //  사용한 버퍼를 지운다
                redisTemplate.opsForList().trim(feedBufferKey, pageSize, -1);

                return new SliceImpl<>(result, pageable, true);
            }

            // 피드 버퍼에 글이 없다면, 50개를 가져온다.
            String feedCursorKey = RedisKey.FEED_CURSOR.makeKey(memberId);
            Object value = redisTemplate.opsForValue().get(feedCursorKey);
            Long cursor = value != null ? Long.parseLong(value.toString()) : Long.MAX_VALUE;

            List<Content> contents = contentRepository.findAllWithAuthorByCursor(cursor, pageable);

            // 빈 페이지시 방어 코드
            if (contents.isEmpty()) {
                return new SliceImpl<>(Collections.emptyList(), pageable, false);
            }

            // 커서 저장
            Long lastId = contents.get(contents.size()-1).getId();
            redisTemplate.opsForValue().set(feedCursorKey, String.valueOf(lastId));

            //  가져온 글들을 정렬한다.
            String viewRedisKey = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(memberId);
            String likeRedisKey = RedisKey.MEMBER_CATEGORY_VIEW_COUNT.makeKey(memberId);

            Map<ContentCategory, Integer> userViewVector = redisTemplate.opsForHash().entries(viewRedisKey)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> ContentCategory.from(String.valueOf(entry.getKey())),
                            entry -> Integer.parseInt(String.valueOf(entry.getValue()))
                    ));

            Map<ContentCategory, Integer> userLikeVector = redisTemplate.opsForHash().entries(likeRedisKey)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            entry -> ContentCategory.from(String.valueOf(entry.getKey())),
                            entry -> Integer.parseInt(String.valueOf(entry.getValue()))
                    ));

           // 글들을 기준에 따라 정렬한다.
            List<Content> sortedContents = new ArrayList<>(contents);

            double maxLikeLog = contents.stream()
                    .mapToDouble(c -> Math.log1p(c.getLikeCount()))
                    .max()
                    .orElse(1.0);

            Map<Content, Double> scoreMap = contents.stream()
                            .collect(Collectors.toMap(
                               c -> c,
                               c -> {

                                   Map<ContentCategory, Integer> viewVector = new HashMap<>();
                                   viewVector.put(c.getCategory(), 1);
                                   double viewSim = SimilarityUtil.calculateCosineSimilarity(viewVector, userViewVector);

                                   Map<ContentCategory, Integer> likeVector = new HashMap<>();
                                   likeVector.put(c.getCategory(), 1);
                                   double likeSim = SimilarityUtil.calculateCosineSimilarity(likeVector, userLikeVector);

                                   double popularity = SimilarityUtil.calculatePopularityWithLogScaling(c.getLikeCount().doubleValue(), maxLikeLog);
                                   return SimilarityUtil.calculateEuclidDistanceFromIdealStatus(viewSim, likeSim, popularity);
                               }
                            ));

            sortedContents.sort(Comparator.comparingDouble(scoreMap::get));

            //  나머지 글들을 Redis List 순서대로 Push
            if (sortedContents.size() > pageSize) {
                List<Long> leftIds = sortedContents.subList(pageSize, sortedContents.size()).stream()
                        .map(Content::getId)
                        .toList();

                //  rightPushAll을 이용해 한 번의 네트워크 I/O로 처리 (N+1 최적화)
                redisTemplate.opsForList().rightPushAll(feedBufferKey, leftIds.toArray());
            }

            //  저장한 나머지 글들을 리턴한다.
            return new SliceImpl<>(sortedContents.subList(0, Math.min(pageSize, sortedContents.size())).stream()
                    .map(ContentResponseDto::of).toList()
                    , pageable
                    , true);

        }

        return contentRepository.findAllWithAuthor(pageable)
                .map(ContentResponseDto::of);
    }

    @Transactional(readOnly = true)
    public ContentResponseDto read (Long id, MemberDto memberDto) {

        Content content = contentRepository.findByIdWithAuthor(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (memberDto != null) {

            Long memberId = memberDto.getId();
            
            // 중복 조회 확인

            String logKey = RedisKey.VIEW_LOG.makeKey(memberId, id);

            Boolean isFirstView = redisTemplate.opsForValue().setIfAbsent(logKey, "1", 10, TimeUnit.MINUTES);

            //  조회 수 증가
            if (Boolean.TRUE.equals(isFirstView)) {

                String redisKey = RedisKey.MEMBER_CATEGORY_VIEW_COUNT.makeKey(memberId);
                redisTemplate.opsForHash().increment(redisKey, content.getCategory(), 1);

                redisTemplate.expire(redisKey
                        , RedisKey.MEMBER_CATEGORY_VIEW_COUNT.getTtl()
                        , RedisKey.MEMBER_CATEGORY_VIEW_COUNT.getTimeUnit());

                redisTemplate.expire(logKey
                        , RedisKey.VIEW_LOG.getTtl()
                        , RedisKey.VIEW_LOG.getTimeUnit());
            }

        }

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
        if (files != null && !files.isEmpty()) {
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

        Content content = contentRepository.findByIdWithAuthor(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

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

        Content content = contentRepository.findByIdWithAuthor(id).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));

        if (!content.getAuthor().getId().equals(memberDto.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        List<ContentImage> files = content.getFiles();

        for (ContentImage file : files) {
            fileHandler.deleteFile(file.getStoreFilename());
        }

        contentRepository.delete(content);

    }

    @Transactional
    public Long addLike(
            Long contentId,
            MemberDto memberDto) {

        // 글과 회원이 존재하는가?
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // Redis Set 사용!
        String userLikeKey = RedisKey.LIKE_CONTENT_USERS.makeKey(contentId);

        //  Redis Set에 유저 ID 추가 시도
        //  add() 결과 값: 1 = 새로 추가됨(성공), 0 = 이미 있음(중복)
        Long isAdded = redisTemplate.opsForSet().add(userLikeKey, member.getId());

        if (isAdded != null && isAdded == 0) {
            //  이미 Set에 들어있다면 중복 클릭임 -> 예외 던짐
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        //  3. DB 저장 (영속성 유지를 위해 DB에도 저장은 함)
        //  (단, 위에서 Redis로 중복을 막았으니 DB 조회 쿼리 없이 바로 save만 하면됨)
        //  *주의: 혹시 Redis 데이터가 날아갔을 경우를 대비해 try-catch로 DB 중복 에러를 잡는 방어 코드를 넣기도 함.
        ContentLike contentLike = ContentLike.builder()
                .content(content)
                .member(member)
                .build();
        contentLikeRepository.save(contentLike);

        //  4. Redis 카운트 증가 (기본 로직 유지)
        String countKey = RedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        redisTemplate.opsForValue().increment(countKey);

        //  5. Redis 일일 랭킹 카운트 증가
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.opsForZSet().incrementScore(RedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today), contentId, 1);

        // 게시글 좋아요 더티 체킹
        redisTemplate.opsForSet().add(RedisKey.LIKE_UPDATED_CONTENTS.makeKey(), contentId);

        // 유저별 카운트 증가
        String MEMBER_CATEGORY_LIKE_COUNT = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(member.getId());

        redisTemplate.opsForHash().increment(MEMBER_CATEGORY_LIKE_COUNT, content.getCategory(), 1);

        return contentLike.getId();
    }

    @Transactional
    public void removeLike(
            Long contentId,
            MemberDto memberDto) {

        // 글과 회원이 존재하는가?
        Content content = contentRepository.findById(contentId).orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        Member member = memberRepository.findById(memberDto.getId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // Redis Set에서 유저 삭제
        String userLikeKey = RedisKey.LIKE_CONTENT_USERS.makeKey(contentId);

        //  remove() 결과값: 1 = 삭제됨, 0 = 원래 없었음
        Long isRemoved = redisTemplate.opsForSet().remove(userLikeKey, member.getId());

        if (isRemoved != null && isRemoved == 0) {
            //  이미 Set에 들어있다면 중복 클릭임 -> 예외 던짐
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }
        
        //  DB 삭제
        contentLikeRepository.deleteByContentAndMember(content, member);
        
        //  Redis 카운트 감소
        String countKey = RedisKey.LIKE_CONTENT_COUNT.makeKey(contentId);
        redisTemplate.opsForValue().decrement(countKey);

        //  Redis 일일 랭킹 카운트 감소
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.opsForZSet().incrementScore(RedisKey.LIKE_DAILY_RANKING_COUNT.makeKey(today), contentId, -1);
        
        // 게시글 좋아요 더치 체킹
        redisTemplate.opsForSet().add(RedisKey.LIKE_UPDATED_CONTENTS.makeKey(), contentId);

        // 유저별 카운트 증가
        String MEMBER_CATEGORY_LIKE_COUNT = RedisKey.MEMBER_CATEGORY_LIKE_COUNT.makeKey(member.getId());

        redisTemplate.opsForHash().increment(MEMBER_CATEGORY_LIKE_COUNT, content.getCategory(), -1);
    }

}
