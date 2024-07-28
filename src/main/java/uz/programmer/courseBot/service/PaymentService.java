package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.Cart;
import uz.programmer.courseBot.model.Order;

import java.util.*;

@Service
public class PaymentService {

    private final CartService cartService;

    private final OrderService orderService;

    private static String TESTKEY = "UGF5Y29tOlNTVXVTOWtQWEtGVmdENlFpeUFpY0hCSU9FZlpCc0ZSQG9AdQ==";

    Map<String, Object> transactionNotFound = Map.of(
            "en", "Transaction is not found",
            "ru", "Транзакция не найдена",
            "uz", "Tranzaksiya topilmadi"
    );

    Map<String, Object> result = new HashMap<>();

    @Autowired
    public PaymentService(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
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

        if (bearerToken == null || !verifyBearerToken(bearerToken)) {
            addError(
                    -32504,
                    Map.of(
                            "en", "Incorrect Authorization",
                            "ru", "Неправильная Авторизация",
                            "uz", "Notogri Avtorizatsiya"
                    )
            );
            return result;
        }

        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);

        if (jsonObject.has("method")) {
            String method = jsonObject.get("method").getAsString();
            JsonObject params = jsonObject.get("params").getAsJsonObject();
            switch (method) {
                case "CheckPerformTransaction": {
                    result = new HashMap<>();
                    if (verifyAmountAndAccountData(params)) {
                        addSuccess(
                                Map.of(
                                        "allow", true
                                )
                        );
                    }
                    break;
                }
                case "CreateTransaction" : {
                    result = new HashMap<>();
                    if (verifyAmountAndAccountData(params)) {
                        int cartId = params.get("account").getAsJsonObject().get("user_id").getAsInt();
                        String transactionId = params.get("id").getAsString();

                        Optional<Order> pendingOrder = orderService.findOrderByTransactionId(transactionId);
                        if (pendingOrder.isPresent()) {
                            addSuccess(
                                    Map.of(
                                            "create_time", pendingOrder.get().getCreateTime(),
                                            "transaction", pendingOrder.get().getTransactionId(),
                                            "state", pendingOrder.get().getState()
                                    )
                            );
                        } else {
                            Order order = orderService.save(cartId, transactionId, 1);
                            addSuccess(
                                    Map.of(
                                            "create_time", order.getCreateTime(),
                                            "transaction", order.getTransactionId(),
                                            "state", order.getState()
                                    )
                            );
                        }
                    }
                    break;
                }
                case "PerformTransaction" : {
                    result = new HashMap<>();
                    String transactionId = params.get("id").getAsString();

                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                        if (order.get().getState() == 1 || order.get().getState() == 2) {
                            if (order.get().getPerformTime() == 0) {
                                order.get().setPerformTime(System.currentTimeMillis());
                                order.get().setState(2);
                                orderService.update(order.get());
                            }

                            addSuccess(
                                    Map.of(
                                            "transaction", order.get().getTransactionId(),
                                            "perform_time", order.get().getPerformTime(),
                                            "state", order.get().getState()
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
                    result = new HashMap<>();

                    String transactionId = params.get("id").getAsString();
                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                        int state = order.get().getState();

                        if (state == 1 || state == 2) {
                            //updates state and cancel_time
                            order.get().setState(-state);
                            order.get().setCancelTime(System.currentTimeMillis());
                            if (params.has("reason")) {
                                order.get().setReason(params.get("reason").getAsInt());
                            }
                            orderService.update(order.get());

                            Map<String, Object> cancelMap = new HashMap<>(Map.of(
                                    "transaction", order.get().getTransactionId(),
                                    "cancel_time", order.get().getCancelTime(),
                                    "state", order.get().getState()
                            ));

                            if (params.has("reason"))
                                cancelMap.put("reason", order.get().getReason());
                            addSuccess(cancelMap);
                        } else {
                            Map<String, Object> cancelMap = new HashMap<>(Map.of(
                                    "transaction", order.get().getTransactionId(),
                                    "cancel_time", order.get().getCancelTime(),
                                    "state", order.get().getState()
                            ));

                            if (order.get().getReason() != null)
                                cancelMap.put("reason", order.get().getReason());

                            addSuccess(cancelMap);
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
                    result = new HashMap<>();

                    String transactionId = params.get("id").getAsString();
                    Optional<Order> order = orderService.findOrderByTransactionId(transactionId);

                    if (order.isPresent()) {
                        Map<String, Object> successMap = new HashMap<String, Object>(Map.of(
                                "create_time", order.get().getCreateTime(),
                                "perform_time", order.get().getPerformTime(),
                                "cancel_time", order.get().getCancelTime(),
                                "transaction", order.get().getTransactionId(),
                                "state", order.get().getState()
                        ));

                        successMap.put("reason", order.get().getReason());
                        addSuccess(successMap);
                    } else {
                        addError(
                                -31003,
                                transactionNotFound
                        );
                        return result;
                    }
                    break;
                }
                case "GetStatement" : {
                    if (params.has("from") && params.has("to")) {
                        long from = params.get("from").getAsLong();
                        long to = params.get("to").getAsLong();

                        List<Order> orders = orderService.findTransactionsByDate(from, to);
                        List<Map<String, Object>> transactions = new ArrayList<>();

                        for (Order order : orders) {
                            Map<String, Object> transaction = new HashMap<>(Map.of(
                                    "id", order.getTransactionId(),
                                    "create_time", order.getCreateTime(),
                                    "perform_time", order.getPerformTime(),
                                    "cancel_time", order.getCancelTime(),
                                    "transaction", order.getId(),
                                    "state", order.getState()
                            ));
                            transaction.put("reason", null);
                            transactions.add(transaction);
                        }

                        addSuccess(Map.of("transactions", transactions));
                    }
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
    }

    private boolean verifyAmountAndAccountData(JsonObject params) {
        if (params.has("account")) {
            JsonObject account = params.get("account").getAsJsonObject();
            if (account.has("user_id")) {
                int cartId = Integer.parseInt(account.get("user_id").getAsString());
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
                                        "en", "Amount is incorrect"
                                )
                        );
                        return false;
                    }
                } else {
                    addError(
                            -31050,
                            Map.of(
                                    "ru", "Номер корзины не найден",
                                    "uz", "Cart raqami mavjud emas",
                                    "en", "Cart number is not found"
                            )
                    );
                    return false; // Immediate return on error
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
                return false; // Immediate return on error
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
            return false; // Immediate return on error
        }
    }

    private boolean verifyBearerToken(String bearerToken) {
        return bearerToken.substring(6).equals(TESTKEY);
    }
}
