package uz.programmer.courseBot.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name= "lesson")
@NoArgsConstructor
public class CourseLesson {
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id", referencedColumnName = "id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private CourseSection courseSection;
}
