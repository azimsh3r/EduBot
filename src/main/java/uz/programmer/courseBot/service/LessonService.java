package uz.programmer.courseBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.CourseLesson;
import uz.programmer.courseBot.repository.CourseLessonRepository;

import java.util.Optional;

@Service
public class LessonService {
    private final CourseLessonRepository lessonRepository;

    @Autowired
    public LessonService(CourseLessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public Optional<CourseLesson> findById(int lessonId) {
        return lessonRepository.findById(lessonId);
    }
}
