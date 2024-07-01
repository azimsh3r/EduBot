package uz.programmer.courseBot.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.programmer.courseBot.dto.AuthPrincipalDTO;
import uz.programmer.courseBot.exception.ExceptionResponse;
import uz.programmer.courseBot.exception.InvalidCredentialsException;
import uz.programmer.courseBot.security.JWTUtil;
import uz.programmer.courseBot.service.AuthenticationService;
import uz.programmer.courseBot.util.ValidationExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JWTUtil jwtUtil;
    private final ValidationExceptionHandler validationExceptionHandler;

    public AuthenticationController(AuthenticationService authenticationService, JWTUtil jwtUtil, ValidationExceptionHandler validationExceptionHandler) {
        this.authenticationService = authenticationService;
        this.jwtUtil = jwtUtil;
        this.validationExceptionHandler = validationExceptionHandler;
    }

    @PostMapping("/login")
    public void login(@RequestBody String response) {
        authenticationService.processMessage(response);
    }

    @PostMapping("/processLogin")
    public ResponseEntity<?> performRegistration(
            @Valid @RequestBody AuthPrincipalDTO authPrincipal,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return validationExceptionHandler.handleValidationException(bindingResult);
        }

        try {
            authenticationService.verifyOTP(authPrincipal);
        } catch (InvalidCredentialsException ex) {
            return new ResponseEntity<>(new ExceptionResponse(ex.getMessage() != null ? ex.getMessage() : "Authentication Failed! Unauthorized!"), HttpStatus.BAD_REQUEST);
        }

        String jwt = jwtUtil.generateToken(authPrincipal.getToken());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
