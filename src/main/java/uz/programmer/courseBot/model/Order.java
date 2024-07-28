package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Calendar;

@Getter
@Setter
@Entity
@Table(name= "bot_order")
public class Order {
    public Order (String transactionId, Cart cart, int state) {
        this.transactionId = transactionId;
        this.cart = cart;
        this.state = state;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "transaction_id")
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "cart_id", referencedColumnName = "id")
    private Cart cart;

    @Column(name="state")
    private Integer state;

    @Column(name="create_time")
    private Long createTime = System.currentTimeMillis();

    @Column(name="perform_time")
    private Long performTime = 0L;

    @Column(name = "cancel_time")
    private Long cancelTime = 0L;

    @Column(name = "reason")
    private Integer reason = null;

    public Order() {}
}
