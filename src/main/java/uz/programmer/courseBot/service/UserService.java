package uz.programmer.courseBot.service;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findUserByPhoneNumber(phoneNumber);
    }

    public Optional<User> findUserByChatId(int chatId) {
        return userRepository.findUserByChatId(chatId);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public void setState(int chatId, String state) {
        Optional<User> user = findUserByChatId(chatId);
        user.ifPresent(value -> value.setState(state));
    }

    public void setPrevState(int chatId, String state) {
        Optional<User> user = findUserByChatId(chatId);
        user.ifPresent(value -> value.setPreviousState(state));
    }

    public boolean verifyRegistration(int chatId) {
        return userRepository.findUserByChatId(chatId).isPresent();
    }

    public boolean verifyPhoneNumber(JsonObject message, int chatId) {
        JsonObject contact = message.getAsJsonObject("contact");
        if (contact != null && contact.has("user_id")) {
            return contact.get("user_id").getAsInt() == chatId;
        }
        return false;
    }

    public User extractUserFromMessage(JsonObject message) {
        User user = new User();
        JsonObject chat = message.getAsJsonObject("chat");
        if (chat != null) {
            user.setFirstName(chat.has("first_name") ? chat.get("first_name").getAsString() : null);
            user.setLastName(chat.has("last_name") ? chat.get("last_name").getAsString() : null);
            user.setChatId(chat.has("id") ? chat.get("id").getAsInt() : 0);
        }

        JsonObject contact = message.getAsJsonObject("contact");
        if (contact != null) {
            user.setPhoneNumber(contact.has("phone_number") ? contact.get("phone_number").getAsString() : null);
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().startsWith("+")) {
                user.setPhoneNumber("+" + user.getPhoneNumber());
            }
        }
        return user;
    }

    public Optional<User> findUserByPhoneNumber(int userId) {
        return userRepository.findById(userId);
    }

    public void updateBoughtCourses(List<Course> courses, int userId) {
        Optional<User> user = userRepository.findById(userId);

        user.ifPresent(u -> {
            u.setBoughtCourses(courses);
            userRepository.save(u);
        });
    }
}
