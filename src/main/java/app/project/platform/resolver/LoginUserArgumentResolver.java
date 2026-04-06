package app.project.platform.resolver;

import app.project.platform.annotation.LoginUser;
import app.project.platform.domain.code.ErrorCode;
import app.project.platform.domain.dto.MemberDto;
import app.project.platform.entity.Member;
import app.project.platform.exception.BusinessException;
import app.project.platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        //  파라미터에 @LoginUser 어노테이션이 붙어있고, 타입이 MemberDeto일 때만 동작!
        boolean hasAnnotation = parameter.hasMethodAnnotation(LoginUser.class);
        boolean isMemberDtoType = MemberDto.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isMemberDtoType;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        return MemberDto.from(member);

    }
}
