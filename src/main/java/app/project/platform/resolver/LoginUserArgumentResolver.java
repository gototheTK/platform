package app.project.platform.resolver;

import app.project.platform.annotation.LoginUser;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        //  파라미터에 @LoginUser 어노테이션이 붙어있고, 타입이 MemberDto일 때만 동작!
        boolean hasAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean isMemberDtoType = MemberDto.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isMemberDtoType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        //  SecurityContextHolder.getContext().getAuthentication().getPrincipal()는 없으면 null 아니라 "anonymousUser"라는 String값을 내뱉는다.
        if (!(principal instanceof MemberDto memberDto)) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        log.info("🎯 [Resolver] 시큐리티 컨텍스트에서 꺼낸 유저 ID: {}, 이메일: {}, 권한: {}", memberDto.getId(), memberDto.getEmail(), memberDto.getRole());

        return memberDto;

    }
}
