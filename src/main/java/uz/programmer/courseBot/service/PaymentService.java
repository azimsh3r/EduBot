package uz.programmer.courseBot.service;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uz.programmer.courseBot.model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {
    BotService botService;

    UserService userService;

    @Autowired
    PaymentService(BotService botService, UserService userService) {
        this.botService = botService;
        this.userService = userService;
    }

    private Map<String, Object> getRequestMap(String method, Map<String, Object> params) {
        Map<String, Object> map = new HashMap<>();
        map.put("method", method);
        map.put("params", params);
        return map;
    }

    public void pay (int amount, int chatId) {
        Optional<User> user = userService.findUserByChatId(chatId);
        if (user.isPresent()) {
            try {
                checkPerformTransaction(amount, user.get().getPhoneNumber());
            } catch (Exception e) {
                botService.sendTelegramMessage(e.getMessage(), chatId, new HashMap<>());
            }
        }
    }

    private void checkPerformTransaction (int amount, String phoneNumber) {
        Map<String, Object> params = Map.of(
                "amount", amount,
                "account", Map.of(
                        "phone", phoneNumber
                )
        );
        try {
            sendRequest(getRequestMap("CheckPerformTransaction", params));
        } catch (Exception e) {
            //TODO: Handle this error here
        }
    }

    private void createTransaction (int amount, String phoneNumber) {
        Map<String, Object> params = Map.of (
                "id", "id here",//Yet to be received
                "amount", amount,
                "account", Map.of("phone", phoneNumber),
                "time", LocalDateTime.now()
        );

        try {
            sendRequest(getRequestMap("CreateTransaction", params));
        } catch (Exception e) {
            //TODO: Handle Exceptions here
        }
    }

    private void performTransaction () {
        try {
            sendRequest (getRequestMap("PerformTransaction", Map.of("id", "id here")));
        } catch (Exception e) {
            System.out.println();
        }
    }

    private void cancelTransaction () {
        Map<String, Object> params = Map.of(
                "id", "id here",
                "reason", 1
        );

        sendRequest(getRequestMap("CancelTransaction", params));
    }

    private void checkTransaction() {
        try {
            sendRequest(getRequestMap("CheckTransaction", Map.of("id", "id here")));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getStatement() {
        Map<String, Object> params = Map.of(
                "from", LocalDateTime.now().minusMinutes(1),
                "to", LocalDateTime.now()
        );

        try {
            sendRequest(getRequestMap("GetStatement", params));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private JsonObject sendRequest(Map<String, Object> map) {
        return RestClient
                .builder()
                .baseUrl("url here")
                .build()
                .post()
                .uri("/endpoints here")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + System.getenv("payme_token"))
                .body(map)
                .retrieve()
                .body(JsonObject.class);
    }
}
