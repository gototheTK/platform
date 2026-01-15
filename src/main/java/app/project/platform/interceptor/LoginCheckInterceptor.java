package app.project.platform.interceptor;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// 스프링에서 아용할 수 있는, 인터셉트 인터페이스를 상속받는다.
public class LoginCheckInterceptor implements HandlerInterceptor {


    // 왜 Filter가 아니라 Interceptor인가?
    // 1. Filter는 스프링 컨테이너 바깥에 있어서 스프링 빈 제어가 까다롭기 때문이다. 그리고 Interceptor는 스프링 빈이라 나중에 Service 주입 등이 쉽다.
    // 2. 컨트롤러 매핑 정보(HandlerMethod)를 알 수 있어서 더  정교한 로직 이가능

    // 로그인 체크
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("LOGIN_MEMBER") == null) {

            if (request.getMethod().equals("GET")) {
              return true;
            }

            throw new BusinessException(ErrorCode.UNAUTHORIZED);

        }

        return true;

    }
}
