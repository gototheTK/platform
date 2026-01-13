package app.project.platform.interceptor;

import app.project.platform.domain.code.ErrorCode;
import app.project.platform.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// 스프링에서 아용할 수 있는, 인터셉트 인터페이스를 상속받는다.
public class LoginCheckInterceptor implements HandlerInterceptor {

    // 로그인 체크
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession();

        if (session == null || session.getAttribute("LOGIN_MEMBER") == null) {

            if (request.getMethod().equals("GET")) {
              return true;
            }

            throw new BusinessException(ErrorCode.UNAUTHORIZED);

        }

        return true;

    }
}
