package com.example.pdfchatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${llm.api.key:}")
    private String apiKey;
    
    @Value("${llm.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${llm.model:gpt-3.5-turbo}")
    private String model;
    
    @Value("${llm.enabled:true}")
    private boolean enabled;
    
    public LlmService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateAnswer(String question, List<String> contextChunks) {
        if (!enabled || apiKey == null || apiKey.trim().isEmpty()) {
            // Fallback to simple text extraction if LLM is not configured
            return generateFallbackAnswer(question, contextChunks);
        }
        
        try {
            // Build the prompt with context
            StringBuilder context = new StringBuilder();
            context.append("You are a helpful assistant that answers questions based on the following context from PDF documents.\n\n");
            context.append("Context from PDFs:\n");
            
            for (int i = 0; i < contextChunks.size() && i < 5; i++) {
                context.append("\n[Document ").append(i + 1).append("]\n");
                context.append(contextChunks.get(i));
                context.append("\n");
            }
            
            context.append("\n\nQuestion: ").append(question);
            context.append("\n\nAnswer the question based solely on the provided context. ");
            context.append("If the context doesn't contain enough information to answer the question, ");
            context.append("say so. Be concise and accurate in your response.");
            
            // Build the request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant that answers questions based on provided context from PDF documents.");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", context.toString());
            
            List<Map<String, String>> messages = List.of(systemMessage, userMessage);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);
            
            // Make the API call
            String apiKeyValue = (apiKey != null && !apiKey.isEmpty()) ? apiKey : "";
            String apiUrlValue = (apiUrl != null && !apiUrl.isEmpty()) ? apiUrl : "https://api.openai.com/v1/chat/completions";
            
            if (apiKeyValue.isEmpty()) {
                return generateFallbackAnswer(question, contextChunks);
            }
            
            String response = webClient.post()
                    .uri(apiUrlValue)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKeyValue)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            // Parse the response
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText().trim();
                    }
                }
            }
            
            return "I apologize, but I couldn't generate a proper response. Please try again.";
            
        } catch (Exception e) {
            System.err.println("Error calling LLM API: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple answer generation
            return generateFallbackAnswer(question, contextChunks);
        }
    }
    
    private String generateFallbackAnswer(String question, List<String> contextChunks) {
        if (contextChunks.isEmpty()) {
            return "I couldn't find relevant information to answer your question.";
        }
        
        // Simple fallback: return the most relevant chunk
        String bestMatch = contextChunks.get(0);
        
        // Try to extract relevant sentences
        String questionLower = question.toLowerCase();
        String[] questionWords = questionLower.split("\\s+");
        
        String[] sentences = bestMatch.split("[.!?]+");
        StringBuilder answer = new StringBuilder();
        
        for (String sentence : sentences) {
            if (sentence == null || sentence.trim().isEmpty()) continue;
            String sentenceLower = sentence.toLowerCase();
            for (String word : questionWords) {
                if (word != null && word.length() > 3 && sentenceLower.contains(word)) {
                    if (answer.length() > 0) answer.append(" ");
                    answer.append(sentence.trim());
                    break;
                }
            }
        }
        
        if (answer.length() > 0) {
            String result = answer.toString();
            return result.length() > 500 ? result.substring(0, 500) + "..." : result;
        }
        
        return bestMatch.length() > 500 ? bestMatch.substring(0, 500) + "..." : bestMatch;
    }
}

