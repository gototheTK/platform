package app.project.platform.config;

import app.project.platform.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
// OncePerRequestFilter를 쓰면은, 한번의 요청당 한번의 필터를 타는 것을 보장한다. 포워드시 불필요하게 필터를 타는것을 방지한다.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //  1. HTTP 헤더에서 'Authorization' 값을 꺼냅니다.
        String authorization = request.getHeader("Authorization");

        //  2. 토큰이 없거나, 'Bearer '로 시작하지 않으면 바로 다음 필터로 넘깁니다. (인증 실패 상태로 진행)
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        //  3. 'Bearer ' 부분을 잘라내고 순수 토큰 문자열만 추출합니다.
        String token = authorization.split(" ")[1];

        //  4. JwtUtil을 사용해 토큰이 유효한지 검증합니다.
        if (jwtUtil.validateAccessToken(token)) {

            //  5. 유효하다면 토큰에서 이메일과 권한을 꺼냅니다.
            String email = jwtUtil.getEmailFromAccessToken(token);
            String role = jwtUtil.getRoleFromAccessToken(token);

            //  6. 스프링 시큐리티가 알아들을 수 있는 '인증 객체'를 만듭니다
            //  Role 앞에는 관례적으로 "ROLE_" 접두사를 붙여줍니다.
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            //  7. 스프링 시큐리티의 SecurityContext(보안 컨텍스트)에 인증 객체를 저장합니다.
            //  이제부터 이 요청은 '인증된 사용자'가 보낸 것으로 취급됩니다.
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        //  8. 처리가 끝났으니 다음 필터로 요청을 넘깁니다.
        filterChain.doFilter(request, response);

    }
}
