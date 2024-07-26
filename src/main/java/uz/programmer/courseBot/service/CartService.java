package uz.programmer.courseBot.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.Cart;
import uz.programmer.courseBot.model.Course;
import uz.programmer.courseBot.model.User;
import uz.programmer.courseBot.repository.CartRepository;

import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
public class CartService {
    private final CartRepository cartRepository;

    private final CourseService courseService;

    @Autowired
    public CartService(CartRepository cartRepository, CourseService courseService) {
        this.cartRepository = cartRepository;
        this.courseService = courseService;
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

    public Cart save(Cart newCart) {
        return cartRepository.save(newCart);
    }

    public Optional<Cart> findCartByUserId(int id) {
        return cartRepository.findCartByUserId(id);
    }

    public Optional<Cart> findCartByCartId(int id) {
        return cartRepository.findById(id);
    }
}
