package uz.programmer.courseBot.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;

@Component
public class CourseDAO {
    private final JdbcTemplate jdbcTemplate;

    public CourseDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addCourseToBoughtCourses(Course course, User user) {
        jdbcTemplate.update("INSERT INTO course_user (user_id, course_id) values (?, ?)", user.getId(), course.getId());
    }
}
