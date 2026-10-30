package com.example.exam.service;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-3.8-flash}}") String model,
            @Value("${gemini.timeout-ms:${GEMINI_TIMEOUT_MS:15000}}") int timeoutMs) {

        this.model = model;

        if (apiKey == null || apiKey.isBlank()) {
            this.client = null;
            return;
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(timeoutMs).build())
                .build();
    }

    public String generateResponse(String userMessage) {

        if (client == null) {
            return "The AI assistant is not configured. Please contact the administrator.";
        }

        String systemPrompt = """
                You are the AI Exam Assistant for an Online Examination System.

                Your job is to help students with:
                - examinations
                - exam instructions
                - subjects
                - course enrollment
                - payments
                - examination results
                - Java
                - Python
                - CSE
                - computer science
                - programming
                - data structures
                - algorithms
                - databases
                - web development

                Give simple, accurate and useful answers suitable for
                college and M.Tech students.

                Keep answers reasonably concise.

                Do not claim that you performed an action in the application
                unless the application actually provides that functionality.
                """;

        String prompt = systemPrompt
                + "\n\nStudent question:\n"
                + userMessage;

        /*
         * Keep the model configurable because Google retires model aliases.
         */
        try {
            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .maxOutputTokens(300)
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            model,
                            prompt,
                            config
                    );

            String text = response.text();
            return text != null && !text.isBlank()
                    ? text
                    : "I couldn't generate an answer. Please try again.";
        } catch (Exception e) {
            System.err.println("Gemini request failed: " + e.getMessage());
            return "The AI assistant is temporarily unavailable. Please try again.";
        }
    }
}