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

        if (memberRepository.count() > 0) return;

        Member author = Member.builder()
                .username("test")
                .password(passwordEncoder.encode("test"))
                .email("test@test.co.kr")
                .build();

        memberRepository.save(author);

        Content content1 = Content.builder()
                .title("돌아온 개발자")
                .description("스프링 부트와 함께하는 좌충우돌 성장기")
                .category(ContentCategory.WEBTOON)
                .author(author)
                .build();

        Content content2 = Content.builder()
                .title("이세계 알고리즘")
                .description("알고리즘으로 몬스터를 잡는 소설")
                .category(ContentCategory.NOVEL)
                .author(author)
                .build();

        contentRepository.saveAll(List.of(content1, content2));

    }

}
