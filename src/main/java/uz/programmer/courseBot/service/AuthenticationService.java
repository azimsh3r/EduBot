package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import uz.programmer.courseBot.dto.AuthPrincipalDTO;
import uz.programmer.courseBot.exception.InvalidCredentialsException;
import uz.programmer.courseBot.model.AuthPrincipal;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.AuthenticationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class AuthenticationService {

    private final AuthenticationRepository authenticationRepository;
    private final UserService userService;

    @Autowired
    public AuthenticationService(AuthenticationRepository authenticationRepository, UserService userService) {
        this.authenticationRepository = authenticationRepository;
        this.userService = userService;
    }

    private static final String BOT_TOKEN = "6992753580:AAEcWmO8lFQV8OAoezml-A9c8tEj12QNufM";

    public int generateOTP() {
        return new Random().nextInt(899999) + 100000;
    }

    public void verifyOTP(@Valid AuthPrincipalDTO authPrincipal) {
        List<AuthPrincipal> userList = authenticationRepository.findAllByToken(authPrincipal.getToken());
        if (userList.isEmpty() || userList.get(0).getOtp() != authPrincipal.getOtp()) {
            throw new InvalidCredentialsException();
        } else if (userList.get(0).getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("The one-time password expired!");
        }
    }

    public void processMessage(String response) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        JsonObject message = jsonObject.getAsJsonObject("message");
        if (message == null) {
            message = jsonObject.getAsJsonObject("my_chat_member");
        }

        if (message != null) {
            String text = message.has("text") ? message.get("text").getAsString() : "Invalid";
            JsonObject chat = message.getAsJsonObject("chat");

            if (chat != null) {
                String name = chat.has("first_name") ? chat.get("first_name").getAsString() : "Xurmatli Foydalanuvchi";
                int chatId = chat.has("id") ? chat.get("id").getAsInt() : 0;

                if (text.contains("/start") && text.length() > 7) {
                    handleStartCommand(text, name, chatId);
                } else if (response.contains("\"contact\":{\"phone_number\"") && verifyPhoneNumber(message, chatId)) {
                    handleContactMessage(message, chatId);
                }
            }
        }
    }

    private void handleStartCommand(String text, String name, int chatId) {
        sendMessage("Salom " + name + " 👋\nBotga xush kelibsiz\n⬇️ Kontaktingizni yuboring (tugmani bosib)", chatId, true);

        String token = text.substring(text.indexOf("/start ") + 7);

        List<AuthPrincipal> regAuthPrincipalList = authenticationRepository.findAllByChatId(chatId);

        AuthPrincipal authPrincipal;
        if (!regAuthPrincipalList.isEmpty()) {
            authPrincipal = regAuthPrincipalList.get(0);
            authPrincipal.setToken(token);
        } else {
            authPrincipal = new AuthPrincipal();
            authPrincipal.setChatId(chatId);
            authPrincipal.setToken(token);
        }

        authenticationRepository.save(authPrincipal);
    }

    private void handleContactMessage(JsonObject message, int chatId) {
        int otp = generateOTP();

        User user = extractUserDataFromMessage(message);

        List<User> regUserList = userService.findAllByPhoneNumber(user.getPhoneNumber());

        if (!regUserList.isEmpty()) {
            user.setId(regUserList.get(0).getId());
        }
        userService.save(user);

        List<AuthPrincipal> regAuthPrincipalList = authenticationRepository.findAllByChatId(chatId);
        AuthPrincipal authPrincipal = new AuthPrincipal();
        authPrincipal.setOtp(otp);
        authPrincipal.setUser(user);

        if (!regAuthPrincipalList.isEmpty()) {
            authPrincipal.setId(regAuthPrincipalList.get(0).getId());
            authPrincipal.setChatId(regAuthPrincipalList.get(0).getChatId());
            authPrincipal.setToken(regAuthPrincipalList.get(0).getToken());
        }
        authenticationRepository.save(authPrincipal);

        sendMessage("Sizning parolingiz: " + otp, chatId, false);
    }

    private User extractUserDataFromMessage(JsonObject message) {
        User user = new User();
        JsonObject chat = message.getAsJsonObject("chat");

        if (chat != null) {
            user.setFirstName(chat.has("first_name") ? chat.get("first_name").getAsString() : null);
            user.setLastName(chat.has("last_name") ? chat.get("last_name").getAsString() : null);
        }

        JsonObject contact = message.getAsJsonObject("contact");
        if (contact != null) {
            user.setPhoneNumber(contact.has("phone_number") ? contact.get("phone_number").getAsString() : null);
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().startsWith("+")) {
                user.setPhoneNumber("+" + user.getPhoneNumber());
            }
        }

        user.setRole("ROLE_USER");
        return user;
    }

    private void sendMessage(String text, int chatId, boolean requestContact) {
        Map<String, Object> keyboard = Map.of(
                "keyboard", List.of(
                                List.of(
                                    Map.of(
                                        "text", "Get Contact",
                                            "request_contact", requestContact
                                    )
                                )
                        ),
                "resize_keyboard", true,
                "one_time_keyboard", true
        );

        RestClient
                .builder()
                .baseUrl("https://api.telegram.org/bot"+BOT_TOKEN)
                .build()
                .post()
                .uri("/sendMessage")
                .accept(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "chat_id", chatId,
                                "text", text,
                                "reply_markup", keyboard
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }

    private boolean verifyPhoneNumber(JsonObject message, int chatId) {
        JsonObject contact = message.getAsJsonObject("contact");
        if (contact != null && contact.has("user_id")) {
            int userId = contact.get("user_id").getAsInt();
            return userId == chatId;
        }
        return false;
    }
}
