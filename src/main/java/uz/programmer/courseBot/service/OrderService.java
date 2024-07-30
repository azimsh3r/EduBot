package uz.programmer.courseBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uz.programmer.courseBot.dao.OrderDAO;
import uz.programmer.courseBot.model.Order;
import uz.programmer.courseBot.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderDAO orderDAO;

    private final CartService cartService;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderDAO orderDAO, CartService cartService) {
        this.orderRepository = orderRepository;
        this.orderDAO = orderDAO;
        this.cartService = cartService;
    }

    public Order save (int cartId, String transactionId, int state) {
        Order order = new Order(transactionId, cartService.findCartByCartId(cartId).get(), state);
        order.setCreateTime(System.currentTimeMillis());
        return orderRepository.save(order);
    }

    public Optional<Order> findOrderByTransactionId (String transactionId) {
        return orderRepository.findByTransactionId(transactionId);
    }

    public void update(Order order) {
        orderRepository.save(order);
    }

    public List<Order> findTransactionsByDate(long from, long to) {
        return orderDAO.findTransactionsFromTo(from, to);
    }


    public List<Order> findAllByCartIdAndStateAndTransactionId(int cartId, int state, String transactionId) {
        return orderRepository.findOrderByCartIdAndState(cartId, state, transactionId, LocalDateTime.now());
    }
}
