package uz.programmer.courseBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="authentication_principal")//, schema = "moykachi")
@Setter
@Getter
public class AuthPrincipal {


    @Column(name = "otp")
    private int otp;

    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @OneToOne
    private User user;

    @Column(name="token")
    String token = null;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private int id;

    @Column(name = "expires_at")
    private final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(1);

    @Column(name = "chat_id")
    private int chatId;
}