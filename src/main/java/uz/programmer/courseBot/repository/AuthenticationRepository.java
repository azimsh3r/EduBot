package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.AuthPrincipal;

import java.util.List;

@Repository
public interface AuthenticationRepository extends JpaRepository<AuthPrincipal, Integer> {
    List<AuthPrincipal> findAllByToken(String token);

    List<AuthPrincipal> findAllByChatId(int chatId);
}
