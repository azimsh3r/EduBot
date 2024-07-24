package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uz.programmer.courseBot.model.Cart;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.CourseSection;
import uz.programmer.courseBot.model.User;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class BotService {
    private final UserService userService;

    private final PaymentService paymentService;

    private final CartService cartService;

    private final CourseService courseService;

    private final RestClient restClient = RestClient.builder().baseUrl("https://api.telegram.org/bot" + BOT_TOKEN).build();

    @Autowired
    public BotService(UserService userService, PaymentService paymentService, CartService cartService, CourseService courseService) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.cartService = cartService;
        this.courseService = courseService;
    }

    private static final String BOT_TOKEN = System.getenv("coursebot_token");

    public void handleIncomingMessage(String response) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        JsonObject message = jsonObject.getAsJsonObject("message");

        if (jsonObject.has("my_chat_member")) {
            message = jsonObject.getAsJsonObject("my_chat_member");
        } else if (jsonObject.has("callback_query")) {
            message = jsonObject.getAsJsonObject("callback_query").getAsJsonObject("message");
        }

        if (message != null) {
            JsonObject chat = message.getAsJsonObject("chat");

            if (chat != null) {
                processCommandMessage(jsonObject, message, chat);
            }
        }
    }

    private void processCommandMessage(JsonObject response, JsonObject message, JsonObject chat) {
        String text = message.has("text") ? message.get("text").getAsString() : "Invalid";
        int chatId = chat.has("id") ? chat.get("id").getAsInt() : 0;

        if (text.equals("/start")) {
            String name = chat.has("first_name") ? chat.get("first_name").getAsString() : "User";
            startUserSession(name, chatId);
            return;
        } else if (message.has("contact")) {
            processUserContact(message, chatId);
            return;
        } else if (response.has("callback_query")) {
            processCallbackData(response, chatId);
            return;
        }

        if (userService.verifyRegistration(chatId)) {
            User user = userService.findUserByChatId(chatId).get();

            switch (text) {
                case "All Courses\uD83E\uDDD1\u200D\uD83D\uDCBB️", "My Courses": {
                    if (text.equals("All Courses\uD83E\uDDD1\u200D\uD83D\uDCBB️")) {
                        userService.setState(chatId, "courses");
                    } else {
                        userService.setState(chatId, "mycourses");
                    }
                    userService.setPrevState(chatId, "start");
                    displayCourses(user);
                    break;
                }
                case "My Cart\uD83D\uDED2" : {
                    userService.setState(chatId, "mycart");
                    userService.setPrevState(chatId, "start");
                    viewCart(user);
                    break;
                }
                case "Go Back⬅️": {
                    navigateBack(user);
                    break;
                }
                default: {
                    String state = user.getState();
                    if (state.equals("courses") || state.equals("mycourses") || state.startsWith("course")) {
                        displayCourseDetails(courseService.findCourseByTitle(text), user, false, 0);
                    }
                }
            }
        } else {
            Map<String, Object> replyMarkup = Map.of(
                    "keyboard", List.of(List.of(Map.of("text", "Get Contact", "request_contact", true))),
                    "resize_keyboard", true,
                    "one_time_keyboard", true
            );

            sendTelegramMessage("Firstly, make sure to login by sending your contact details!", chatId, replyMarkup);
        }
    }

    private void startUserSession(String name, int chatId) {
        userService.setState(chatId, "start");

        if (userService.verifyRegistration(chatId)) {
            displayMainMenu(chatId);
        } else {
            Map<String, Object> replyMarkup = Map.of(
                    "keyboard", List.of(List.of(Map.of("text", "Get Contact", "request_contact", true))),
                    "resize_keyboard", true,
                    "one_time_keyboard", true
            );
            sendTelegramMessage("Hello " + name + "! Please send your contacts to proceed!", chatId, replyMarkup);
        }
    }

    private void processUserContact(JsonObject message, int chatId) {
        if (!userService.verifyPhoneNumber(message, chatId)) {
            sendTelegramMessage("Invalid Contact Details! Please send your own contact details!", chatId, new HashMap<>());
            return;
        }

        User user = userService.extractUserFromMessage(message);
        user.setState("courses");

        Optional<User> regUser = userService.findUserByPhoneNumber(user.getPhoneNumber());

        regUser.ifPresent(value -> user.setId(value.getId()));
        User newUser = userService.save(user);

        //Create a new cart as soon as registered
        Cart cart = new Cart();
        cart.setUser(newUser);
        cartService.save(cart);

        displayMainMenu(chatId);
    }

    private void displayMainMenu(int chatId) {
        List<Map<String, Object>> commandList = List.of(
                Map.of("text", "All Courses\uD83E\uDDD1\u200D\uD83D\uDCBB️"),
                Map.of("text", "My Courses"),
                Map.of("text", "My Cart\uD83D\uDED2")
        );

        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(commandList),
                "is_persistent", true,
                "resize_keyboard", true,
                "one_time_keyboard", true
        );
        sendTelegramMessage("Please choose any option from menu below!", chatId, replyMarkup);
    }

    private void displayCourses(User user) {
        List<Course> courseList = new ArrayList<>();

        courseList = switch (user.getState()) {
            case "courses" -> courseService.findAll();
            case "mycourses" -> user.getBoughtCourses();
            default -> courseList;
        };

        List<Map<String, Object>> listOfButtons = new ArrayList<>();
        courseList.forEach(course -> listOfButtons.add(Map.of("text", course.getTitle())));

        if (user.getState().equals("courses"))
            listOfButtons.add(Map.of("text", "My Cart\uD83D\uDED2"));
        listOfButtons.add(Map.of("text", "Go Back⬅️"));

        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(listOfButtons),
                "resize_keyboard", true,
                "one_time_keyboard", false
        );
        sendTelegramMessage("Please Choose any course from options below!", user.getChatId(), replyMarkup);
    }

    private void viewCart(User user) {
        Optional<Cart> cart = cartService.findCartByUserId(user.getId());
        List<Map<String, Object>> inlineButtons = new ArrayList<>();

        Map<String, Object> replyMarkup = new HashMap<>(Map.of(
                "is_persistent", true,
                "resize_keyboard", true,
                "one_time_keyboard", true
        ));

        if (cart.isPresent() && !cart.get().getCourseList().isEmpty()) {
            replyMarkup.put("keyboard", List.of(List.of(
                    Map.of("text", "Go Back⬅️")
            )));

            sendTelegramMessage("Please choose any option from menu below!", user.getChatId(), replyMarkup);

            StringBuilder text = new StringBuilder();
            List<Course> courses = cart.get().getCourseList();

            for (int i = 0; i < courses.size(); i++) {
                text.append(i+1).append(") ").append(courses.get(i).getTitle()).append("\n");
                inlineButtons.add(Map.of("text", "❌" + courses.get(i).getTitle(), "callback_data", "delete_in_cart " + courses.get(i).getId()));
            }
            text.append("Total Amount: ").append(cart.get().getTotalAmount()).append(" sum");
            inlineButtons.add(Map.of("text", "Buy Courses!\uD83D\uDCB3", "url", paymentService.pay(user.getId())));

            sendTelegramMessage(
                    text.toString(),
                    user.getChatId(),
                    Map.of(
                        "inline_keyboard", List.of(inlineButtons)
                    )
            );
        } else {
            replyMarkup.put("keyboard", List.of(List.of(
                    Map.of("text", "Go Back⬅️")
            )));
            sendTelegramMessage("Your cart is empty!", user.getChatId(), replyMarkup);
        }
    }

    private void editCart(User user, int messageId) {
        Hibernate.initialize(user.getCart());
        StringBuilder text = new StringBuilder();

        List<Map<String, Object>> inlineButtons = new ArrayList<>();
        List<Course> courses = user.getCart().getCourseList();

        if (courses.isEmpty()) {
            editTelegramMessage("Your cart is empty!", user.getChatId(), messageId, new HashMap<>());
        } else {
            for (int i = 0; i < courses.size(); i++) {
                text.append(i + 1).append(") ").append(courses.get(i).getTitle()).append("\n");
                inlineButtons.add(Map.of("text", "❌" + courses.get(i).getTitle(), "callback_data", "delete_in_cart " + courses.get(i).getId()));
            }
            text.append("Total Amount: ").append(user.getCart().getTotalAmount()).append(" sum");
            inlineButtons.add(Map.of("text", "Buy Courses!\uD83D\uDCB3", "url", paymentService.pay(user.getId())));

            editTelegramMessage(
                    text.toString(),
                    user.getChatId(),
                    messageId,
                    Map.of(
                            "inline_keyboard", List.of(inlineButtons)
                    )
            );
        }
    }

    private void displayCourseDetails(Optional<Course> course, User user, boolean edit, int messageId) {
        course.ifPresent(value -> {
            userService.setPrevState(user.getChatId(), "start");
            userService.setState(user.getChatId(), "course " + value.getId());

            List<Map<String, Object>> inlineButtons = new ArrayList<>();

            Hibernate.initialize(user.getBoughtCourses());
            Hibernate.initialize(user.getCart());

            if (user.getBoughtCourses().contains(course.get())) {
                inlineButtons.add(Map.of("text", "View", "callback_data", "view_course " + value.getId()));
            } else if (user.getCart().getCourseList().contains(course.get())) {
                inlineButtons.add(Map.of("text", "❌\uD83D\uDDD1", "callback_data", "delete_out_cart " + value.getId()));
            } else {
                inlineButtons.add(Map.of("text", "\uD83D\uDED2", "callback_data", "add_cart " + value.getId()));
            }

            if (edit) {
                editTelegramMessage(
                        "\uD83D\uDCDA" + value.getTitle() + "\n\uD83D\uDC64 By " + value.getAuthor() + "\nRanking: " + value.getRanking() + "\nPrice(In soums): " +value.getPrice(),
                        user.getChatId(),
                        messageId,
                        Map.of(
                                "inline_keyboard", List.of(inlineButtons),
                                "is_persistent", true,
                                "resize_keyboard", true,
                                "one_time_keyboard", true
                        )
                );
            } else {
                sendTelegramMessage(
                        "\uD83D\uDCDA" + value.getTitle() + "\n\uD83D\uDC64 By " + value.getAuthor() + "\nRanking: " + value.getRanking() + "\nPrice(In soums): " +value.getPrice(),
                        user.getChatId(),
                        Map.of(
                                "inline_keyboard", List.of(inlineButtons),
                                "is_persistent", true,
                                "resize_keyboard", true,
                                "one_time_keyboard", true
                        )
                );
            }
        });
    }

    private void displayCourseSectionDetails(int courseId, User user, int messageId) {
        Optional<Course> course = courseService.findCourseById(courseId);

        StringBuilder sectionsString = new StringBuilder();
        if (course.isPresent() && !course.get().getCourseSectionList().isEmpty()) {
            List<Map<String, Object>> inlineButtons = new ArrayList<>();

            List<CourseSection> courseSections = course.get().getCourseSectionList();

            sectionsString.append("Sections:").append("\n");
            for (int i = 0; i < courseSections.size(); i++) {
                CourseSection section = courseSections.get(i);
                Hibernate.initialize(section.getCourseLessonList());

                sectionsString.append(i + 1).append(") ").append(section.getName()).append(" (").append(section.getCourseLessonList().size()).append(" lessons)").append("\n");
                inlineButtons.add(Map.of(
                        "text", section.getName(),
                        "callback_data", "view_section " + i + " course " + course.get().getId()
                ));
            }
            editTelegramMessage(sectionsString.toString(), user.getChatId(), messageId, Map.of("inline_keyboard", List.of(inlineButtons)));
        } else {
            sendTelegramMessage("Error, no Sections found!", user.getChatId(), new HashMap<>());
        }
    }

    private void navigateBack(User user) {
        String prevState = user.getPreviousState();
        if (prevState.equals("start")) {
            userService.setState(user.getChatId(), "start");
            displayMainMenu(user.getChatId());
        } else {
            if (prevState.startsWith("course ")) {
                //TODO: Logic for going back to course details
            }
        }
    }

    private void processCallbackData(JsonObject response, int chatId) {
        JsonObject callbackQuery = response.getAsJsonObject("callback_query");

        Optional<User> user = userService.findUserByChatId(chatId);

        if (callbackQuery.has("data") && user.isPresent()) {
            int messageId = callbackQuery.get("message").getAsJsonObject().get("message_id").getAsInt();

            String data = callbackQuery.get("data").getAsString();

            Matcher deleteOutOfCartMatcher = Pattern.compile("delete_out_cart\\s*(\\d+)").matcher(data);
            Matcher deleteInCartMatcher = Pattern.compile("delete_in_cart\\s*(\\d+)").matcher(data);
            Matcher addMatcher = Pattern.compile("add_cart\\s*(\\d+)").matcher(data);
            Matcher viewMatcher = Pattern.compile("view_course\\s*(\\d+)").matcher(data);
            Matcher viewSectionMatcher = Pattern.compile("view_section\\s*(\\d+)\\s*course\\s*(\\d*)").matcher(data);

            if (deleteInCartMatcher.matches()) {
                int courseId = Integer.parseInt(deleteInCartMatcher.group(1));
                cartService.deleteCourseByUserId(courseId, user.get().getId());
                editCart(user.get(), messageId);
            } else if (deleteOutOfCartMatcher.matches()) {
                int courseId = Integer.parseInt(deleteOutOfCartMatcher.group(1));
                cartService.deleteCourseByUserId(courseId, user.get().getId());
                displayCourseDetails(courseService.findCourseById(courseId), user.get(), true, messageId);
            } else if (addMatcher.matches()) {
                int courseId = Integer.parseInt(addMatcher.group(1));
                cartService.addCourseByUserId(courseId, user.get());
                displayCourseDetails(courseService.findCourseById(courseId), user.get(), true, messageId);
            } else if (viewMatcher.matches()) {
                int courseId = Integer.parseInt(viewMatcher.group(1));
                displayCourseSectionDetails(courseId, user.get(), messageId);
            } else if (viewSectionMatcher.matches()) {
                int sectionId = Integer.parseInt(viewSectionMatcher.group(1));
                int courseId = Integer.parseInt(viewSectionMatcher.group(2));

                //TODO: Think about design and how you
            }
        }
    }

    public void sendTelegramMessage(String text, int chatId, Map<String, Object> replyMarkup) {
        try {
            restClient
                    .post()
                    .uri("/sendMessage")
                    .accept(MediaType.APPLICATION_JSON)
                    .body(
                            Map.of(
                                    "chat_id", chatId,
                                    "text", text,
                                    "reply_markup", replyMarkup
                            )
                    )
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.Unauthorized errorException) {
            System.out.println(errorException.getMessage());
        }
    }

    private void editTelegramMessage(String text, int chatId, int messageId, Map<String, Object> replyMarkup) {
        restClient
                .post()
                .uri("/editMessageText")
                .accept(MediaType.APPLICATION_JSON)
                .body(
                        Map.of(
                                "chat_id", chatId,
                                "message_id", messageId,
                                "text", text,
                                "reply_markup", replyMarkup
                        )
                )
                .retrieve()
                .body(String.class);
    }
}
