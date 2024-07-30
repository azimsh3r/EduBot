package uz.programmer.courseBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.programmer.courseBot.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Integer> {
    Optional<Order> findByTransactionId(String transactionId);

    @Query("From Order where cart.id = :cartId and state = :state and transactionId <> :transactionId and timeout > :now")
    List<Order> findOrderByCartIdAndState(@Param("cartId") int cartId, @Param("state") int state, @Param("transactionId") String transactionId, @Param("now") LocalDateTime now);
}
