package com.example.exam.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final Client client;

    public GeminiService() {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not configured."
            );
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public String generateResponse(String userMessage) {

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
         * Google currently documents gemini-3.8-flash for Java.
         */
        String model = "gemini-3.8-flash";

        /*
         * Retry temporary 503/429/network failures.
         */
        int maxAttempts = 3;

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                GenerateContentConfig config =
                        GenerateContentConfig.builder()
                                .maxOutputTokens(500)
                                .build();

                GenerateContentResponse response =
                        client.models.generateContent(
                                model,
                                prompt,
                                config
                        );

                String text = response.text();

                if (text != null && !text.isBlank()) {
                    return text;
                }

                return "I couldn't generate an answer. Please try again.";

            } catch (Exception e) {

                lastException = e;

                System.err.println(
                        "Gemini attempt "
                                + attempt
                                + "/"
                                + maxAttempts
                                + " failed: "
                                + e.getMessage()
                );

                if (attempt < maxAttempts) {

                    try {

                        /*
                         * Exponential backoff:
                         * attempt 1 -> wait 2 seconds
                         * attempt 2 -> wait 4 seconds
                         */
                        long delay =
                                (long) Math.pow(2, attempt) * 1000;

                        System.out.println(
                                "Retrying Gemini in "
                                        + delay
                                        + " ms..."
                        );

                        Thread.sleep(delay);

                    } catch (InterruptedException interruptedException) {

                        Thread.currentThread().interrupt();

                        throw new RuntimeException(
                                "Gemini request interrupted.",
                                interruptedException
                        );
                    }
                }
            }
        }

        /*
         * Gemini is temporarily unavailable.
         * Return a useful response instead of crashing the chatbot.
         */
        System.err.println(
                "Gemini unavailable after "
                        + maxAttempts
                        + " attempts."
        );

        return """
                I'm temporarily unable to connect to the AI service.

                You can still use the examination system normally.
                Please try your question again in a few moments.
                """;
    }
}