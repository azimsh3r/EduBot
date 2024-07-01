package uz.programmer.courseBot.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
public class SuccessResponse {
    private String message;
    private final LocalDateTime timeStamp = LocalDateTime.now();
}
