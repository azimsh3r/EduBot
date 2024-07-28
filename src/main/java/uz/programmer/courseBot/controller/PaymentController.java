package uz.programmer.courseBot.controller;

import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.programmer.courseBot.service.PaymentService;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController (PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public ResponseEntity<Object> receivePaymentResponse (@RequestBody String request, @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        System.out.println(request);
        return new ResponseEntity<>(paymentService.processRequest(request, bearerToken), HttpStatus.OK);
    }
}
