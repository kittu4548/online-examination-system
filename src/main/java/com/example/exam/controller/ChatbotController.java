package com.example.exam.controller;

import com.example.exam.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiService geminiService;

    public ChatbotController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "reply",
                            "Please enter a question."
                    ));
        }

        try {
            String reply = geminiService.generateResponse(
                    message.trim()
            );

            return ResponseEntity.ok(
                    Map.of("reply", reply)
            );

        } catch (Exception e) {

            System.err.println(
                    "Chatbot error: " + e.getMessage()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "reply",
                            "Sorry, I couldn't process your request. Please try again."
                    )
            );
        }
    }
}