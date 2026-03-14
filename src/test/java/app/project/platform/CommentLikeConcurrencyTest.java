package app.project.platform;

import app.project.platform.domain.RedisKey;
import app.project.platform.domain.dto.CommentRequestDto;
import app.project.platform.domain.dto.CommentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.type.Role;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.repository.MemberRepository;
import app.project.platform.service.CommentService;
import app.project.platform.service.ContentService;
import app.project.platform.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
public class CommentLikeConcurrencyTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Tag("load-test")
    @DisplayName("동시성 테스트: 1000명의 유저가 동시에 하나의 댓글에 좋아요를 누른다")
    public void concurrentAddCommentLikeTest1000() throws InterruptedException {

        int threadCount = 1000;

        //DB 세팅
        Long contentId = jdbcTemplate.queryForObject("SELECT max(id) FROM content", Long.class);

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .contentId(contentId)
                .text("text")
                .build();

        MemberDto memberDtoForComment = MemberDto.builder().id(1L).build();
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDtoForComment);

        //  given);
        Long commentId = commentResponseDto.getId(); // 미리 DB에 생성해둔 테스트용 게시글 ID
        String redisLikeCommentUserSet = RedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserQueue = RedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);
        String redisLikeCommentCount = RedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);

        //  1. 테스트 전 Redis 상태를 깔끔하게 청소 (멱등성 보장)
        redisTemplate.delete(redisLikeCommentUserSet);
        redisTemplate.delete(redisLikeCommentUserQueue);
        redisTemplate.delete(redisLikeCommentCount);

        //  2. 유저 1000명 미리 세팅 (CPU 병목 방지를 위해 스레드 밖에서 순차적으로 처리

        Integer recentId = jdbcTemplate.queryForObject("SELECT max(id) FROM member", Integer.class);

        int start = recentId == null ? 1 : recentId+1;
        int end = threadCount + start -1;

        List<Member> members = new ArrayList<>();
        for (int i=start; i<=end; i++) {

            Member member = Member.builder()
                    .email("test"+i+"@eamil.com")
                    .nickname("test"+i)
                    .password("test"+i)
                    .role(Role.USER)
                    .build();

            ReflectionTestUtils.setField(member, "id", (long) i);

            members.add(member);
        }

        // 데이터베이스  회원 적재
        String insertSql = "INSERT INTO member(" +
                "id" +
                ", email" +
                ", nickname" +
                ", password" +
                ", role" +
                ")  " +
                "VALUES(?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(insertSql, members, members.size(), (ps, member) -> {

            ps.setLong(1, member.getId());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getNickname());
            ps.setString(4, member.getPassword());
            ps.setString(5, member.getRole().getName());

        });

        //  32개의 스레드가 동시에 작업을 처리하도록 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        //  1000개의 작업이 모두 끝날 때까지 메인 스레드를 기다리게 하는 장치
        CountDownLatch latch = new CountDownLatch(threadCount);

        //  when
        for (Member member : members) {

            executorService.submit(() -> {
                try {
                    MemberDto memberDto = MemberDto.builder().id(member.getId()).build();
                    commentService.addLike(commentId, memberDto);
                } catch (Exception e) {
                    log.error("에러 발생: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

        }

        //  모든 스레드의 작업이 끝나서 CountDownLatch가 0이 될 때 까지 대기
        latch.await();

        //  then
        //  1. Redis에서 targetContentId의 좋아요 Set 사이즈를 조회해서 1000개인지 확인
        Long likeCount = redisTemplate.opsForSet().size(redisLikeCommentUserSet);

        assertThat(likeCount).isEqualTo(threadCount);

        // 수동 롤백 코드
        jdbcTemplate.update("DELETE FROM member WHERE id >= ?", (long) start);
        jdbcTemplate.update("DELETE FROM comment WHERE id = ?", commentId);

        log.debug("1000개의 동시 좋아요 요청 테스트 종료!");

    }

    @Test
    //@Disabled
    @Tag("load-test")
    @DisplayName("동시성 테스트: 50000명의 유저가 동시에 하나의 댓글에 좋아요를 누른다")
    public void concurrentAddCommentLikeTest50000() throws InterruptedException {

        int threadCount = 50000;

        //DB 세팅
        Long content = jdbcTemplate.queryForObject("SELECT max(id) FROM content", Long.class);

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                .contentId(content)
                .text("text")
                .build();

        MemberDto memberDtoForComment = MemberDto.builder().id(1L).build();
        CommentResponseDto commentResponseDto = commentService.create(commentRequestDto, memberDtoForComment);


        //  given);
        Long commentId = commentResponseDto.getId(); // 미리 DB에 생성해둔 테스트용 게시글 ID
        String redisLikeCommentUserSet = RedisKey.LIKE_COMMENT_USERS_SET.makeKey(commentId);
        String redisLikeCommentUserQueue = RedisKey.LIKE_COMMENT_USERS_QUEUE.makeKey(commentId);
        String redisLikeCommentCount = RedisKey.LIKE_COMMENT_COUNT.makeKey(commentId);

        //  1. 테스트 전 Redis 상태를 깔끔하게 청소 (멱등성 보장)
        redisTemplate.delete(redisLikeCommentUserSet);
        redisTemplate.delete(redisLikeCommentUserQueue);
        redisTemplate.delete(redisLikeCommentCount);

        Integer recentId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM MEMBER", Integer.class);
        int start = recentId==null ? 1 : recentId+1;
        int end = threadCount + start -1;

        List<Member> members = new ArrayList<>();

        String insertSql = "INSERT INTO member (id, " +
                "email," +
                "password," +
                "nickname," +
                "role) " +
                "VALUES(?, ?, ?, ?, ?)";

        for (int i=start; i<=end; i++) {
            Member member = Member.builder()
                    .email("test"+i+"@email.com")
                    .nickname("test"+i)
                    .password("dummy")
                    .role(Role.USER)
                    .build();

            ReflectionTestUtils.setField(member, "id", (long) i);

            members.add(member);
        }

        int size = members.size();

        int chunk = 2000;
        int index = 0;

        while (index<size) {

            List<Member> list = members.subList(index, index+chunk);

            if (list.isEmpty()) break;;

            jdbcTemplate.batchUpdate(insertSql, list, list.size(), (ps, member) ->{

                ps.setLong(1, member.getId());
                ps.setString(2, member.getEmail());
                ps.setString(3, member.getPassword());
                ps.setString(4, member.getNickname());
                ps.setString(5, Role.USER.getName());

            });

            index+=chunk;

        }

        ExecutorService executorService = Executors.newFixedThreadPool(100);

        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (Member member : members) {

            executorService.submit(()->{

                try {
                    MemberDto memberDto = MemberDto.builder().id(member.getId()).build();
                    commentService.addLike(commentId, memberDto);
                } catch (Exception e) {
                    log.error("에러 발행 : {}", e.getMessage());
                } finally {
                    latch.countDown();
                }

            });

        }

        latch.await();

        // then
        Long likeCount = redisTemplate.opsForSet().size(redisLikeCommentUserSet);
        assertThat(likeCount).isEqualTo(threadCount);

        // 수동 돌백
        jdbcTemplate.update("DELETE FROM member WHERE id >= ?", (long) start);
        jdbcTemplate.update("DELETE FROM comment WHERE id = ?", commentId);

        log.debug("50000개의 동시 좋아요 요청 테스트 종료!");

    }

}
