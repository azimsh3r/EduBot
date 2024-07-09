package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.CourseSection;

@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Integer> {}
