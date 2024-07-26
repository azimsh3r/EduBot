package uz.programmer.courseBot.service;

import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.model.Order;
import uz.programmer.courseBot.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final CartService cartService;

    @Autowired
    public OrderService(OrderRepository orderRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    public Order save (int cartId, String transactionId) {
        Order order = new Order(transactionId, cartService.findCartByCartId(cartId).get(), 1);
        order.setCreateTime(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Optional<Order> findOrderByTransactionId (String transactionId) {
        return orderRepository.findByTransactionId(transactionId);
    }

    public void update(Order order) {
        orderRepository.save(order);
    }
}
