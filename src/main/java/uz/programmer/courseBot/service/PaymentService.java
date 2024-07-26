package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.dao.CartDAO;
import uz.programmer.courseBot.model.Cart;
import uz.programmer.courseBot.model.Order;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final CartService cartService;

    private final OrderService orderService;

    private final CartDAO cartDAO;

    Map<String, Object> transactionNotFound = Map.of(
            "en", "Transaction is not found",
            "ru", "Транзакция не найдена",
            "uz", "Tranzaksiya topilmadi"
    );

    Map<String, Object> result = new HashMap<>();

    @Autowired
    public PaymentService(CartService cartService, OrderService orderService, CartDAO cartDAO) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.cartDAO = cartDAO;
    }

    public String pay(int userId) {
        Optional<Cart> cart = cartService.findCartByUserId(userId);

        if(cart.isPresent()) {
            String originalUrl = "m=65745d3ddbf5969676427108" + ";" +
                    "ac.cart_id=" + cart.get().getId() + ";" +
                    "a=" + cart.get().getTotalAmount() * 100 + ";" +
                    "c=https://t.me/prcoursebot";

            String encodedUrl = Base64.getEncoder().encodeToString(originalUrl.getBytes());
            return "https://checkout.paycom.uz/"+encodedUrl;
        } else {
            return "xuynya.ru";
        }
    }

    public Map<String, Object> processRequest(String response, String bearerToken) {
        //TODO: Verify bearer token

        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);

        if (jsonObject.has("method")) {
            String method = jsonObject.get("method").getAsString();
            JsonObject params = jsonObject.get("params").getAsJsonObject();
            switch (method) {
                case "CheckPerformTransaction": {
                    if (verifyAmountAndAccountData(params)) {
                        addSuccess(
                                Map.of(
                                        "allow", true
                                )
                        );
                    }
                }
                case "CreateTransaction" : {
                    if (verifyAmountAndAccountData(params)) {
                        int cartId = params.get("account").getAsJsonObject().get("cart_id").getAsInt();
                        String transactionId = params.get("id").getAsString();

                        orderService.save(cartId, transactionId);
                        addSuccess(
                                Map.of(
                                    "create_time", LocalDate.now(),
                                    "transaction", cartId,
                                    "state", 1
                                )
                        );
                    }
                }
                case "PerformTransaction" : {
                    String transactionId = params.get("id").getAsString();

                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                        if (order.get().getState() == 1) {
                            order.get().setPerformTime(LocalDateTime.now());
                            orderService.update(order.get());

                            addSuccess(
                                    Map.of(
                                            "transaction", order.get().getId(),
                                            "perform_time", LocalDateTime.now(),
                                            "state" , 2
                                    )
                            );
                        } else {
                            addError(
                                    -31050,
                                    Map.of(
                                            "uz", "Mumkin emas",
                                            "ru", "Нельзя",
                                            "en", "Forbidden"
                                    )
                            );
                        }
                    } else {
                        addError(
                                -31003,
                                transactionNotFound
                        );
                    }
                    break;
                }
                case "CancelTransaction" : {
                    String transactionId = params.get("id").getAsString();
                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                        if (order.get().getState() == 1) {
                            order.get().setState(-2);
                            orderService.update(order.get());

                            addSuccess(Map.of(
                                    "transaction", order.get().getId(),
                                    "cancel_time", LocalDateTime.now(),
                                    "state", -2
                            ));
                        } else {
                            addError(-31008, Map.of(
                                    "uz", "Mumkin emas",
                                    "ru", "Невозможно выполнить",
                                    "en", "Impossible to do"
                            ));
                        }
                    } else {
                        addError(
                                -31003,
                                transactionNotFound
                        );
                    }
                    break;
                }
                case "CheckTransaction" : {
                    String transactionId = params.get("id").getAsString();
                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                            addSuccess(
                                    Map.of(
                                            "create_time", order.get().getCreateTime(),
                                            "perform_time", order.get().getPerformTime(),
                                            "cancel_time", 0,
                                            "transaction", order.get().getId(),
                                            "state", order.get().getState(),
                                            "reason", "No reason"
                                    )
                            );
                    } else {
                        addError(
                                -31003,
                                transactionNotFound
                        );
                    }
                    break;
                }
                case "GetStatement" : {
                    //TODO
                    break;
                }
            }
        }
        return result;
    }

    private void addError(int errorCode, Map<String, Object> message) {
        this.result.put(
                "error",
                Map.of(
                        "code", errorCode,
                        "message", message
                )
        );
    }

    private void addSuccess(Map<String, Object> resultMap) {
        result.put("result", resultMap);
//        result.put("id", id);
    }

    private boolean verifyAmountAndAccountData(JsonObject params) {
        if (params.has("account")) {
            JsonObject account = params.get("account").getAsJsonObject();
            if (account.has("cart_id")) {
                int cartId = account.get("cart_id").getAsInt();
                Optional<Cart> cart = cartService.findCartByCartId(cartId);

                if (cart.isPresent()) {
                    int amount = params.get("amount").getAsInt();

                    if (cart.get().getTotalAmount() == amount) {
                        return true;
                    } else {
                        addError(
                                -31001,
                                Map.of(
                                        "ru", "Неверная сумма",
                                        "uz", "Notogri summa",
                                        "en", "Amount is incorrent"
                                )
                        );
                    }
                } else {
                    addError(
                            -31050,
                            Map.of(
                                    "ru", "Номер корзины не найден",
                                    "uz", "Cart raqami topilmadi",
                                    "en", "Cart number is not found"
                            )
                    );
                }
            } else {
                addError(
                        -31050,
                        Map.of(
                                "ru", "Поле аккаунт пустое",
                                "en", "Field account is empty",
                                "uz", "Account bo'sh"
                        )
                );
            }
        }
        return false;
    }
}
