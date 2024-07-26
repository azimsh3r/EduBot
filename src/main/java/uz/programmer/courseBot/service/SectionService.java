package uz.programmer.courseBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.CourseSection;
import uz.programmer.courseBot.repository.CourseSectionRepository;

import java.util.Optional;

@Service
public class SectionService {

    private final CourseSectionRepository courseSectionRepository;

    @Autowired
    public SectionService(CourseSectionRepository courseSectionRepository) {
        this.courseSectionRepository = courseSectionRepository;
    }

    public Optional<CourseSection> findById(int sectionId) {
        return courseSectionRepository.findById(sectionId);
    }
}
