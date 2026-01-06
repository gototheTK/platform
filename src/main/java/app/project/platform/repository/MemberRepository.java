package app.project.platform.repository;

import app.project.platform.dto.MemberDto;
import app.project.platform.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    public Optional<Member> findByUsername(String username);

    public Optional<MemberDto> findByEmailAndPassword(String email, String password);

}
