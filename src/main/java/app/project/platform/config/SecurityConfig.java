package app.project.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔, 정적 리소스, 메인 페이지는 누구나 접근 가능
                        .requestMatchers("/h2-console/**", "/css/**", "/js/**").permitAll()
                        // 나머지는 로그인해야 함
                        .requestMatchers("/", "/member/signup").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable()) // H2 콘솔 깨짐 방지
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")  //H2 콘솔은 CSRF 예외
                )
                .formLogin(form -> form
                        .loginPage("/member/login") // (나중에 만들 페이지)
                        .defaultSuccessUrl("/")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/member/logout")
                        .invalidateHttpSession(true)
                );

        return http.build();
    }

}
