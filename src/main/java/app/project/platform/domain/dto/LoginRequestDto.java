package app.project.platform.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {

    @NotBlank(message = "{member.required.email}")
    @Email(message = "{member.error.emailFormat}")
    private String email;

    @NotBlank(message = "{member.required.password}")
    private String password;

}
