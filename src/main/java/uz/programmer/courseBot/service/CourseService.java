package uz.programmer.courseBot.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.CourseRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;

    private final UserService userService;

    @Autowired
    public CourseService(CourseRepository courseRepository, UserService userService) {
        this.courseRepository = courseRepository;
        this.userService = userService;
    }

    public Optional<Course> findCourseByTitle(String title) {
        return courseRepository.findCourseByTitle(title);
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Optional<Course> findCourseById(int id) {
        return courseRepository.findById(id);
    }

    public void save(Course course) {
        courseRepository.save(course);
    }

    public void deleteFromCartByChatId(int chatId, int course_id) {
        Optional<Course> course = findCourseById(course_id);
        if (course.isPresent()) {
            List<User> cartUserList = course.get().getCartUserList();
            cartUserList.remove(userService.findUserByChatId(chatId).get());
            course.get().setCartUserList(cartUserList);
            System.out.println("done delete");
        }
    }
}
