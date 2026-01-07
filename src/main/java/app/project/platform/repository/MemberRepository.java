package app.project.platform.repository;

import app.project.platform.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    public Optional<Member> findByEmail(String email);

    public Optional<Member> findByNickname(String nickname);

}
