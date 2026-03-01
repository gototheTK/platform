package app.project.platform.entity;

import app.project.platform.domain.type.ContentCategory;
import app.project.platform.domain.type.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ElementCollection
    @CollectionTable (
            name = "member_category_view",
            joinColumns = @JoinColumn(name = "member_id")
    )
    @MapKeyColumn(name = "category")
    @Column(name = "count")
    @Enumerated(EnumType.STRING)
    private Map<ContentCategory, Integer> categoryView = new HashMap<>();

    @ElementCollection
    @CollectionTable(
            name = "member_category_like",
            joinColumns = @JoinColumn(name = "member_id")
    )
    @MapKeyColumn(name = "category")
    @Column(name = "count")
    @Enumerated(EnumType.STRING)
    private Map<ContentCategory, Integer> categoryLike = new HashMap<>();

    @Builder
    public Member(String email, String password, String nickname, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    public void update(String password, String nickname, Role role) {
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

}
