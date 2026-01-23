package app.project.platform.domain.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
