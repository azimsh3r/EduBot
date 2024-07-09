package uz.programmer.courseBot.controller;

import org.springframework.web.bind.annotation.RestController;
import uz.programmer.courseBot.service.PaymentService;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
