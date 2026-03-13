package app.project.platform.init;

import app.project.platform.domain.dto.*;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.service.CommentService;
import app.project.platform.service.ContentService;
import app.project.platform.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberService memberService;

    private final ContentService contentService;

    private final JdbcTemplate jdbcTemplate;

    private final CommentService commentService;

    @Override
    public void run(String... args) throws Exception {

        Integer contentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CONTENT" ,Integer.class);
        Integer commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM COMMENT" ,Integer.class);

        if (contentCount != null && contentCount > 0) return;
        if (commentCount != null && commentCount > 0) return;

        String email = "test@email.com";
        String nickname = "test";
        String password = "password";

        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .email(email)
                .nickname(nickname)
                .password(password)
                .build();

        memberService.signup(signupRequestDto);

        ContentCreateRequestDto contentCreateRequestDto = ContentCreateRequestDto.builder()
                .title("title")
                .description("description")
                .category(ContentCategory.CARTOON.getName())
                .build();

        List<MultipartFile> files = new ArrayList<>();

        MemberDto memberDto = MemberDto.builder()
                .id(1L)
                .email(email)
                .nickname(nickname)
                .role(Role.USER.getName())
                .build();

        Long contentId = contentService.create(contentCreateRequestDto, files, memberDto);

        CommentRequestDto commentRequestDto = CommentRequestDto.builder()
                        .id(1L)
                        .contentId(contentId)
                        .text("text")
                        .build();

        commentService.create(commentRequestDto, memberDto);

    }
}
