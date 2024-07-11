package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name="Course")
public class Course {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="title")
    private String title;

    @Column(name="description")
    private String description;

    @Column(name="author")
    private String author;

    @Column(name="price")
    private int price;

    @Column(name="payment_url")
    private String paymentUrl;

    @Column(name="ranking")
    private int ranking;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "course_user",
            joinColumns = { @JoinColumn(name = "course_id", referencedColumnName = "id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id", referencedColumnName = "id") }
    )
    private List<User> userList;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "course_user_cart",
            joinColumns = { @JoinColumn(name = "course_id", referencedColumnName = "id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id", referencedColumnName = "id") }
    )
    private List<User> cartUserList;

    @OneToMany(mappedBy = "course")
    private List<CourseSection> courseSectionList;

    @OneToMany(mappedBy = "course")
    private List<CourseLesson> courseLessonList;
}
