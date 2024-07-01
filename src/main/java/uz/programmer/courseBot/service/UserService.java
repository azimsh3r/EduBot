package uz.programmer.courseBot.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.UserRepository;
import uz.programmer.courseBot.security.AuthDetails;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void save(User user) {
        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    public void admin() {
        // Admin functionality
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public UserDetails findAllByToken(String token) {
        return new AuthDetails(userRepository.findUserByToken(token));
    }

    public List<User> findAllByPhoneNumber(String phoneNumber) {
        return userRepository.findAllByPhoneNumber(phoneNumber);
    }

    public List<User> findAllById(int id) {
        return userRepository.findAllById(id);
    }
}
