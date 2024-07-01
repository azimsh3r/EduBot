package uz.programmer.courseBot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthPrincipalDTO {
    @NotNull(message = "One-Time password cannot be null")
    //@Size(min=6, max = 6, message = "One-Time password should consist of 6 digits")
    private int otp;

    @NotNull(message = "AuthToken cannot be null!")
    private String token;
}
