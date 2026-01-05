package app.project.platform.init;

import app.project.platform.domain.type.ContentCategory;
import app.project.platform.entity.Content;
import app.project.platform.entity.Member;
import app.project.platform.repository.ContentRepository;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;

    private final ContentRepository contentRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        Member author = Member.builder()
                .username("test")
                .password(passwordEncoder.encode("test"))
                .email("test@test.co.kr")
                .build();

        memberRepository.save(author);

        Content content1 = Content.builder()
                .title("title1")
                .description("decription1")
                .category(ContentCategory.WEBTOON)
                .author(author)
                .build();

        Content content2 = Content.builder()
                .title("title2")
                .description("description2")
                .category(ContentCategory.NOVEL)
                .author(author)
                .build();

        List<Content> list = new ArrayList<>();
        list.add(content1);
        list.add(content2);

        contentRepository.saveAll(list);
    }

}
