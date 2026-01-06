package app.project.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {

    @Email
    @NotBlank(message = "{member.required.email}")
    private String email;

    @NotBlank(message = "{member.required.password}")
    private String password;

}
