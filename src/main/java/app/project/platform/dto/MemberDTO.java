package app.project.platform.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDTO {

    @NotBlank(message = "{member.required.username}")
    private String username;

    @NotBlank(message = "{member.required.password}")
    private String password;

    @NotBlank(message = "{member.required.email}")
    @Email
    private String email;

}
