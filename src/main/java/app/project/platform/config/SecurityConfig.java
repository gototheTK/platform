package app.project.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
// @Component를 쓰면, 내부 빈 메서드를 사용하면 의존 객체를 주입하지않고 새로 생성함. 그러면 싱글톤 패턴이 깨짐
// 싱긍톤 패턴이 깨지면 passwordEncoder를 내부에서 여러번 호출하면, 새로운 객체를 만들게되고
// 그러면 메모리 낭비가 심해지거나 로직오류를 발생할 우려가 있음
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // 1. REST API이므로  CSRF 보호 비활성화 (토큰 방식을 쓰므로 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 기본 폼 로그인 및 기본 HTTP Basic 인증 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 3. 세션을 생성하지 않음(Stateful -> Stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            // 4. API 경로별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                    //  회원가입, 로그인 API는 누구나 접근 가능해야 한다.
                    .requestMatchers(
                            "/h2-console/**",
                            "/api/v1/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                    ).permitAll()
                    //  그 외의 모든 요청은 인증이 필요하다.
                    .anyRequest().authenticated()
            )
            // 5. 커스텀 필터 등록
            // 방금 만든 jwtAuthenticationFilter를 기본 인증 필터인 UsernamePasswordAuthenticationFilter 앞에 끼워 넣습니다.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

}
