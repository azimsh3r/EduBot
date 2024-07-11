package uz.programmer.courseBot.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class BotService {
    private final UserService userService;

    private final CourseService courseService;

    @Autowired
    public BotService(UserService userService, CourseService courseService) {
        this.userService = userService;
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
                case "Add to Cart\uD83D\uDED2": {
                    Pattern pattern = Pattern.compile("course\\s*(\\d+)");
                    Matcher matcher = pattern.matcher(user.getState());

                    if (matcher.matches()) {
                        int id = Integer.parseInt(matcher.group(1));

                        Optional<Course> foundCourse = courseService.findCourseById(id);
                        foundCourse.ifPresent(course -> {
                            List<User> cartUserList = course.getCartUserList();
                            if (!cartUserList.contains(user)) {
                                cartUserList.add(user);
                                course.setCartUserList(cartUserList);
                                courseService.save(course);
                                sendTelegramMessage("Successfully added to your Cart!", chatId, new HashMap<>());
                            } else {
                                sendTelegramMessage("This course is already added to your Cart!", chatId, new HashMap<>());
                            }
                        });
                    }
                    break;
                }
                case "Buy the course!\uD83D\uDCB3": {
                    // TODO: 7/8/2024 Logic for payment
                    break;
                }
                case "View Details": {
                    Pattern pattern = Pattern.compile("course\\s*(\\d+)");
                    Matcher matcher = pattern.matcher(user.getState());

                    if (matcher.matches()) {
                        int courseId = Integer.parseInt(matcher.group(1));
                        displayCourseSectionDetails(courseId);
                    }
                    break;
                }
                default: {
                    String state = user.getState();
                    if (state.equals("courses") || state.equals("mycourses") || state.equals("mycart")) {
                        userService.setPrevState(chatId, user.getState());
                        displayCourseDetails(text, user);
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
        userService.save(user);

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
            case "mycourses" -> user.getBoughtCourseList();
            default -> courseList;
        };

        List<Map<String, Object>> listOfButtons = new ArrayList<>();
        courseList.forEach(course -> listOfButtons.add(Map.of("text", course.getTitle())));

        listOfButtons.add(Map.of("text", "Go Back⬅️"));

        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(listOfButtons),
                "resize_keyboard", true,
                "one_time_keyboard", true
        );
        sendTelegramMessage("Please Choose any course from options below!", user.getChatId(), replyMarkup);
    }

    private void viewCart(User user) {
        List<Course> courseList = user.getCartCourseList();

        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(List.of(
                        Map.of("text", "Buy All Courses"),
                        Map.of("text", "Go Back⬅️")
                )),
                "is_persistent", true,
                "resize_keyboard", true,
                "one_time_keyboard", true
        );

        if(!courseList.isEmpty()) {
           sendTelegramMessage("Your Cart!", user.getChatId(), replyMarkup);
            for (Course course : courseList) {
                sendTelegramMessage(
                        "\uD83D\uDCDA" + course.getTitle() + "\nDescription: " + course.getDescription() + "\n\uD83D\uDC64 By " + course.getAuthor() + "\nRanking: " + course.getRanking() + "\nPrice(In soums): " + course.getPrice(),
                        user.getChatId(),
                        Map.of(
                                "inline_keyboard", List.of(List.of(
                                        Map.of("text", "\uD83D\uDDD1", "callback_data", "cart " + course.getId()),
                                        Map.of("text", "Buy the course!\uD83D\uDCB3", "url", "payme.uz")
                                ))
                        ));
            }
        } else {
            sendTelegramMessage("Your cart is empty!", user.getChatId(), replyMarkup);
        }
    }

    private void displayCourseDetails(String text, User user) {
        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(List.of(
                        Map.of("text", "View Details"),
                        Map.of("text", "Add to Cart\uD83D\uDED2"),
                        Map.of("text", "Buy the course!\uD83D\uDCB3"),
                        Map.of("text", "Go Back⬅️")
                )),
                "is_persistent", true,
                "resize_keyboard", true,
                "one_time_keyboard", true
        );

        Optional<Course> course = courseService.findCourseByTitle(text);
        course.ifPresent(value -> {
            userService.setState(user.getChatId(), "course "+value.getId());
            sendTelegramMessage("\uD83D\uDCDA" + value.getTitle() + "\n\uD83D\uDC64 By " + value.getAuthor() + "\nRanking: " + value.getRanking() + "\nPrice(In soums): " +value.getPrice(), user.getChatId(), replyMarkup);
        });
    }

    private void displayCourseSectionDetails(int courseId) {

    }

    private void navigateBack(User user) {
        String prevState = user.getPreviousState();
        switch (prevState) {
            case "start" : {
                userService.setState(user.getChatId(), "start");
                displayMainMenu(user.getChatId());
                break;
            }
            case "courses", "mycourses", "mycart" : {
                userService.setState(user.getChatId(), user.getPreviousState());
                userService.setPrevState(user.getChatId(), "start");

                displayCourses(user);
                break;
            }
            default: {
                if (prevState.startsWith("course ")) {
                    //TODO: Add a logic to come back from components of a course
                }
            }
        }
    }

    private void processCallbackData(JsonObject response, int chatId) {
        JsonObject callbackQuery = response.getAsJsonObject("callback_query");

        if (callbackQuery.has("data")) {
            String data = callbackQuery.get("data").getAsString();
            Pattern pattern = Pattern.compile("cart\\s*(\\d+)");

            Matcher matcher = pattern.matcher(data);
            if (matcher.matches()) {
                int id = Integer.parseInt(matcher.group(1));
                courseService.deleteFromCartByChatId(chatId, id);
                sendTelegramMessage("Course removed successfully!", chatId, new HashMap<>());
            }
        }
    }

    public void sendTelegramMessage(String text, int chatId, Map<String, Object> replyMarkup) {
        try {
            RestClient
                    .builder()
                    .baseUrl("https://api.telegram.org/bot" + BOT_TOKEN)
                    .build()
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
}
