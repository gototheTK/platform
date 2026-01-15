package app.project.platform.domain.dto;

import app.project.platform.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MemberDto {

    private Long id;

    private String email;

    private String nickname;

    private String role;

    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .role(member.getRole().getName())
                .build();
    }

}
