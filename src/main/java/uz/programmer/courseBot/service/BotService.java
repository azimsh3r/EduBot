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

    private static final String BOT_TOKEN = "6992753580:AAEcWmO8lFQV8OAoezml-A9c8tEj12QNufM";

    public void processMessage(String response) {
        JsonObject jsonObject = new Gson().fromJson(response, JsonObject.class);
        JsonObject message = jsonObject.getAsJsonObject("message");
        if (message == null) {
            message = jsonObject.getAsJsonObject("my_chat_member");
        }

        if (message != null) {
            JsonObject chat = message.getAsJsonObject("chat");

            if (chat != null) {
                handleCommands(message, chat);
            }
        }
    }

    private void handleCommands(JsonObject message, JsonObject chat) {
        String text = message.has("text") ? message.get("text").getAsString() : "Invalid";
        int chatId = chat.has("id") ? chat.get("id").getAsInt() : 0;

        if (text.equals("/start")) {
            String name = chat.has("first_name") ? chat.get("first_name").getAsString() : "User";
            handleStart(name, chatId);
            return;
        } else if (message.has("contact")) {
            handleContact(message, chatId);
            return;
        }

        if (userService.verifyRegistration(chatId)) {
            User user = userService.findUserByChatId(chatId).get();

            switch (text) {
                case "All Courses\uD83E\uDDD1\u200D\uD83D\uDCBB️", "My Courses", "My Cart\uD83D\uDED2": {
                    if (text.equals("All Courses\uD83E\uDDD1\u200D\uD83D\uDCBB️")) {
                        userService.setState(chatId, "courses");
                    } else if (text.equals("My Courses")) {
                        userService.setState(chatId, "mycourses");
                    } else {
                        userService.setState(chatId, "mycart");
                    }

                    userService.setPrevState(chatId, "start");
                    handleCourses(user);
                    break;
                }
                case "Go Back⬅️": {
                    handleGoBack(user);
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
                                sendMessage("Successfully added to your Cart!", chatId, new HashMap<>());
                            } else {
                                sendMessage("This course is already added to your Cart!", chatId, new HashMap<>());
                            }
                        });
                    }
                    break;
                }
                case "Buy the course!\uD83D\uDCB3": {
                    // TODO: 7/8/2024 Logic for payment
                    break;
                }
                default: {
                    String state = user.getState();
                    if (state.equals("courses") || state.equals("mycourses") || state.equals("mycart")) {
                        userService.setPrevState(chatId, user.getState());
                        handleCourse(text, user);
                    }
                }
            }
        } else {
            Map<String, Object> replyMarkup = Map.of(
                    "keyboard", List.of(List.of(Map.of("text", "Get Contact", "request_contact", true))),
                    "resize_keyboard", true,
                    "one_time_keyboard", true
            );

            sendMessage("Firstly, make sure to login by sending your contact details!", chatId, replyMarkup);
        }
    }

    private void handleStart(String name, int chatId) {
        userService.setState(chatId, "start");

        if (userService.verifyRegistration(chatId)) {
            handleMainMenu(chatId);
        } else {
            Map<String, Object> replyMarkup = Map.of(
                    "keyboard", List.of(List.of(Map.of("text", "Get Contact", "request_contact", true))),
                    "resize_keyboard", true,
                    "one_time_keyboard", true
            );
            sendMessage("Hello " + name + "! Please send your contacts to proceed!", chatId, replyMarkup);
        }
    }

    private void handleContact(JsonObject message, int chatId) {
        if (!userService.verifyPhoneNumber(message, chatId)) {
            sendMessage("Invalid Contact Details! Please send your own contact details!", chatId, new HashMap<>());
            return;
        }

        User user = userService.extractUserFromMessage(message);
        user.setState("courses");

        Optional<User> regUser = userService.findUserByPhoneNumber(user.getPhoneNumber());

        regUser.ifPresent(value -> user.setId(value.getId()));
        userService.save(user);

        handleMainMenu(chatId);
    }

    private void handleMainMenu(int chatId) {
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
        sendMessage("Please choose any option from menu below!", chatId, replyMarkup);
    }

    private void handleCourses(User user) {
        List<Course> courseList = new ArrayList<>();
//
//        userService.setPrevState(user.getChatId(), "start");
        switch (user.getState()) {
            case "courses" : {
                courseList = courseService.findAll();
                break;
            }
            case "mycart" : {
                courseList = user.getCartCourseList();
                break;
            }
            case "mycourses" : {
                courseList = user.getBoughtCourseList();
                break;
            }
        }
        List<Map<String, Object>> listOfButtons = new ArrayList<>();
        courseList.forEach(course -> listOfButtons.add(Map.of("text", course.getTitle())));
        listOfButtons.add(Map.of("text", "Go Back⬅️"));

        Map<String, Object> replyMarkup = Map.of(
                "keyboard", List.of(listOfButtons),
                "resize_keyboard", true,
                "one_time_keyboard", true
        );
        sendMessage("Please Choose any course from options below!", user.getChatId(), replyMarkup);
    }

    private void handleCourse(String text, User user) {
        Map<String, Object> replyMarkup = Map.of(
                "inline_keyboard", List.of(List.of(Map.of(
                        "text", "Buy the course!\uD83D\uDCB3",
                        "url", "youtube.com"
                ))),
                "keyboard", List.of(List.of(
                        Map.of("text", "Add to Cart\uD83D\uDED2"),
                        Map.of("text", "Go Back⬅️")
                )),
                "is_persistent", true,
                "resize_keyboard", true,
                "one_time_keyboard", true
        );

        Optional<Course> course = courseService.findCourseByTitle(text);
        course.ifPresent(value -> {
            userService.setState(user.getChatId(), "course "+value.getId());
            sendMessage("\uD83D\uDCDA" + value.getTitle() + "\n\uD83D\uDC64 By " + course.get().getAuthor() + "\nPrice(In soums): " + course.get().getPrice(), user.getChatId(), replyMarkup);
        });
    }

    private void handleGoBack(User user) {
        String prevState = user.getPreviousState();
        switch (prevState) {
            case "start" : {
                userService.setState(user.getChatId(), "start");
                handleMainMenu(user.getChatId());
                break;
            }
            case "courses", "mycourses", "mycart" : {
                userService.setState(user.getChatId(), user.getPreviousState());
                userService.setPrevState(user.getChatId(), "start");

                handleCourses(user);
                break;
            }
            default: {
                if (prevState.startsWith("course ")) {
                    //TODO: Add a logic to come back from components of a course
                }
            }
        }
    }

    private void sendMessage(String text, int chatId, Map<String, Object> replyMarkup) {
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
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Unauthorized errorException) {
            System.out.println(errorException.getMessage());
        }
    }
}
