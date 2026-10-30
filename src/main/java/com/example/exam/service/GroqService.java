
package com.example.exam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GroqService {

    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public GroqService(
            @Value("${groq.api-key:${GROQ_API_KEY:}}")
            String apiKey,

            @Value("${groq.model:${GROQ_MODEL:llama-3.1-8b-instant}}")
            String model,

            @Value("${groq.timeout-ms:20000}")
            int timeoutMs,

            ObjectMapper objectMapper) {

        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.requestTimeout = Duration.ofMillis(timeoutMs);

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(requestTimeout)
                .build();
    }

    public String generateResponse(String userMessage) {

        if (apiKey == null || apiKey.isBlank()) {
            return "The AI assistant is not configured. Set GROQ_API_KEY and restart the application.";
        }

        String systemPrompt = """
                You are the AI Exam Assistant for an Online Examination System.

                Help students with:
                - Examinations and exam instructions
                - Subjects and course enrollment
                - Payments and examination results
                - Java, Python and CSE
                - Computer science and programming
                - Data structures and algorithms
                - Databases and web development

                Give simple, accurate and useful answers suitable for
                college and M.Tech students.

                Keep answers reasonably concise.

                Do not claim that you performed an action in the application
                unless the application actually provides that functionality.
                """;

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            requestBody.put("model", model);
            requestBody.put("max_tokens", 400);

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);

            ObjectNode userMessageNode = messages.addObject();
            userMessageNode.put("role", "user");
            userMessageNode.put("content", userMessage);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            if (status == 429) {
                System.err.println("Groq quota/rate limit exceeded.");
                return "The AI assistant has reached its current usage limit. Please try again later.";
            }

            if (status == 401 || status == 403) {
                System.err.println("Groq authentication failed. HTTP " + status);
                return "The AI assistant credentials were rejected. Check GROQ_API_KEY and restart the application.";
            }

            if (status < 200 || status >= 300) {
                String errorBody = response.body();
                System.err.println("Groq API error. HTTP " + status + ": "
                        + (errorBody == null ? "" : errorBody.substring(0, Math.min(errorBody.length(), 500))));
                return "The AI assistant provider rejected the request (HTTP "
                        + status + "). Check the configured model and API key.";
            }

            JsonNode root = objectMapper.readTree(response.body());

            JsonNode content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode() ||
                    content.isNull() ||
                    content.asText().isBlank()) {

                return "I couldn't generate an answer. Please try again.";
            }

            return content.asText().trim();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Groq request interrupted.");
            return "The AI assistant request was interrupted. Please try again.";

        } catch (Exception e) {
            System.err.println("Groq request failed: " + e.getMessage());
            return "The AI assistant could not connect to Groq. Check network access and try again.";
        }
    }
}
