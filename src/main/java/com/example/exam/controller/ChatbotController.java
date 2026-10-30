
package com.example.exam.controller;

import com.example.exam.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GroqService groqService;

    public ChatbotController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("reply", "Please enter a question."));
        }

        if (message.length() > 2000) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "reply",
                            "Please keep your question below 2000 characters."
                    ));
        }

        String reply = groqService.generateResponse(message.trim());

        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
