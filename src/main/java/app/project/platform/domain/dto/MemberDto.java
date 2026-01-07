package app.project.platform.domain.dto;

import app.project.platform.domain.type.Role;
import app.project.platform.entity.Member;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class MemberDto implements Serializable {

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
