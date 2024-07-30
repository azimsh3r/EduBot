package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findUserByPhoneNumber(String phoneNumber);

    Optional<User> findUserByChatId(int chatId);
}
