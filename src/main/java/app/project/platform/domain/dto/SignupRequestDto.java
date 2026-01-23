package app.project.platform.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupRequestDto {

    @NotBlank(message = "{member.required.username}")
    @Email(message = "{member.error.emailFormat}")
    private String email;

    @NotBlank(message = "{member.required.password}")
    private String password;

    @NotBlank(message = "{member.required.nickname}")
    private String nickname;

}
