package app.project.platform.domain.dto;

import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberDto {

    private Long id;
    private String email;
    private String nickname;
    private Role role;

    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole())
                .build();
    }

}
