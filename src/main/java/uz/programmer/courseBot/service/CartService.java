package uz.programmer.courseBot.service;

import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.dao.CourseDAO;
import uz.programmer.courseBot.model.Cart;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.Order;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.CartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;

    private final CourseService courseService;

    private final CourseDAO courseDAO;

    @Autowired
    public CartService(CartRepository cartRepository, CourseService courseService, CourseDAO courseDAO) {
        this.cartRepository = cartRepository;
        this.courseService = courseService;
        this.courseDAO = courseDAO;
    }

    public void deleteCourseByUserId(int courseId, int userId) {
        Optional<Cart> cart = cartRepository.findCartByUserId(userId).stream().findAny();
        Optional<Course> course = courseService.findCourseById(courseId);
        cart.ifPresent(value -> {
            course.ifPresent(course1 -> {
                value.getCourseList().remove(course1);
                value.setTotalAmount(value.getTotalAmount() - course1.getPrice());
            });
        });
    }

    public void addCourseByUserId(int courseId, User user) {
        Optional<Cart> cart = cartRepository.findCartByUserId(user.getId());
        Optional<Course> course = courseService.findCourseById(courseId);

        if (course.isPresent()) {
            if (!cart.get().getCourseList().contains(course.get())) {
                cart.get().getCourseList().add(course.get());
                cart.get().setTotalAmount(cart.get().getTotalAmount() + course.get().getPrice());
            }
        }
    }

    public void save(Cart newCart) {
        cartRepository.save(newCart);
    }

    public Optional<Cart> findCartByUserId(int id) {
        return cartRepository.findCartByUserId(id);
    }

    public Optional<Cart> findCartByCartId(int id) {
        return cartRepository.findById(id);
    }

    public void obtainAllCourses(Order order) {
        Optional<Cart> cart = cartRepository.findById(order.getCart().getId());
        if (cart.isPresent()) {
            Hibernate.initialize(cart.get().getUser());
            User user = cart.get().getUser();

            Hibernate.initialize(cart.get().getCourseList());
            for (Course course : cart.get().getCourseList()) {
                courseDAO.addCourseToBoughtCourses(course, user);
            }
            cleanCart(cart.get());
        }
    }

    private void cleanCart(Cart cart) {
        cart.setCourseList(new ArrayList<>());
        cart.setTotalAmount(0);
        cartRepository.save(cart);
    }
}
