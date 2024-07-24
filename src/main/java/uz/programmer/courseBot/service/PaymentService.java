package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.dao.CartDAO;
import uz.programmer.courseBot.model.Cart;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final CartService cartService;

    private final CartDAO cartDAO;

    @Autowired
    public PaymentService(CartService cartService, CartDAO cartDAO) {
        this.cartService = cartService;
        this.cartDAO = cartDAO;
    }

    public String pay(int userId) {
        Optional<Cart> cart = cartService.findCartByUserId(userId);
        return cart.map(this::initializePayment).orElse("error.uz");
    }

    private String initializePayment(Cart cart) {
        StringBuilder originalUrl = new StringBuilder();

        originalUrl.append("m=65745d3ddbf5969676427108").append(";");
        originalUrl.append("ac.cart_id=").append(cart.getId()).append(";");
        originalUrl.append("a=").append(cart.getTotalAmount() * 100).append(";");
        originalUrl.append("c=https://t.me/prcoursebot");

        String encodedUrl = Base64.getEncoder().encodeToString(originalUrl.toString().getBytes());
        return "https://checkout.paycom.uz/"+encodedUrl;
    }

    public Map<String, Object> processRequest(String jsonObject) {
        JsonObject response = new Gson().fromJson(jsonObject, JsonObject.class);
        Map<String, Object> result = new HashMap<>();

        if (response.has("method")) {
            String method = response.getAsJsonObject("method").getAsString();
            switch (method) {
                case "CheckPerformTransaction": {
                    result.put(
                            "result",
                            Map.of(
                                    "allow", true
                            )
                    );
                    break;
                }
                case "CreateTransaction" : {
                    result.put(
                            "result",
                            Map.of(
                                    "create_time", LocalDate.now(),
                                    "transaction", 4,
                                    "state", 1
                            )
                    );
                    break;
                }
                case "PerformTransaction" : {
                    result.put(
                            "result",
                            Map.of(
                                    "transaction", 4,
                                    "perform_time", LocalDateTime.now(),
                                    "state" , 2
                            )
                    );
                    break;
                }
                case "CancelTransaction" : {
                    result.put(
                            "result",
                            Map.of(
                                    "transaction", 4,
                                    "cancel_time", LocalDateTime.now(),
                                    "state", -2
                            )
                    );
                    break;
                }
                case "CheckTransaction" : {
                    result.put(
                            "result" ,
                            Map.of(
                                    //TODO: Modify these values to reflect real case
                                    "create_time", LocalDateTime.now(),
                                    "perform_time", LocalDateTime.now(),
                                    "cancel_time", 0,
                                    "transaction", "5123",
                                    "state", 2,
                                    "reason", "No re"
                            )
                    );
                    break;
                }
                case "GetStatement" : {
                    break;
                }
            }
        }
        return result;
    }
}
