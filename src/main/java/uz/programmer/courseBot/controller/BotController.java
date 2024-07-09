package uz.programmer.courseBot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.programmer.courseBot.service.BotService;

@RestController
@RequestMapping("/bot")
public class BotController {
    private final BotService botService;

    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/process")
    public void process(@RequestBody String response) {
        System.out.println(response);
        botService.handleIncomingMessage(response);
    }
}
