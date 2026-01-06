package app.project.platform.dto;


import app.project.platform.entity.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Builder
public class MemberDto implements Serializable {

    private Long id;

    private String username;

    private String email;

    private String role;

    // Entity -> DTO 변환 메서드 (여기서 password는 아예 뺍니다!)
    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .role("ROLE_USER")
                .build();
    }

}
