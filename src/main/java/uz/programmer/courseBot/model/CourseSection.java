package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name= "section")
@Getter
@Setter
@NoArgsConstructor
public class CourseSection {
    @Column(name = "id")
    @Id
    private int id;

    @Column(name="name")
    private String name;

    @OneToMany(mappedBy = "courseSection")
    private List<CourseLesson> courseLessonList;

    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    private Course course;
}
