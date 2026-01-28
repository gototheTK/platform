package app.project.platform.init;

import app.project.platform.domain.dto.ContentCreateRequestDto;
import app.project.platform.domain.dto.ContentResponseDto;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.domain.dto.SignupRequestDto;
import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import app.project.platform.service.ContentService;
import app.project.platform.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberService memberService;

    private final ContentService contentService;

    @Override
    public void run(String... args) throws Exception {

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

        contentService.create(contentCreateRequestDto, files, memberDto);

    }
}
