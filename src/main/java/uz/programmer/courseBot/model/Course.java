package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

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
    private List<User> users;

    @OneToMany(mappedBy = "course")
    private List<CourseSection> courseSectionList;

    @OneToMany(mappedBy = "course")
    private List<CourseLesson> courseLessonList;

    @ManyToMany(mappedBy = "courseList")
    private List<Cart> cartList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return id == course.id && price == course.price && ranking == course.ranking && Objects.equals(title, course.title) && Objects.equals(description, course.description) && Objects.equals(author, course.author) && Objects.equals(paymentUrl, course.paymentUrl) && Objects.equals(users, course.users) && Objects.equals(courseSectionList, course.courseSectionList) && Objects.equals(courseLessonList, course.courseLessonList) && Objects.equals(cartList, course.cartList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, author, price, paymentUrl, ranking, users, courseSectionList, courseLessonList, cartList);
    }
}
