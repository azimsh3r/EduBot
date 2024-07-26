package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="cart")
@Getter
@Setter
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "total_amount")
    private int totalAmount = 0;

    @OneToMany(mappedBy = "cart")
    private List<Order> order;

    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(
            name = "cart_course",
            joinColumns = {@JoinColumn(name = "cart_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name="course_id", referencedColumnName = "id")}
    )
    private List<Course> courseList = new ArrayList<>();
}
