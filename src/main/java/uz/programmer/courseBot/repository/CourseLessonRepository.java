package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.CourseLesson;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, Integer> {}