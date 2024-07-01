package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findAllByPhoneNumber(String phoneNumber);

    @Query("Select u FROM User u JOIN AuthPrincipal ap ON u.id = ap.user.id WHERE ap.token = :token")
    User findUserByToken (@Param("token") String token);

    List<User> findAllById(int id);
}
