package com.example.pdfchatbot.controller;

import com.example.pdfchatbot.service.SimilaritySearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {
    
    @Autowired
    private SimilaritySearchService similaritySearchService;
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> req) {
        String question = req.getOrDefault("question", "");
        
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("answer", "Please provide a question."));
        }
        
        try {
            // Find most relevant chunks
            List<String> relevantChunks = similaritySearchService.findMostRelevantChunks(question, 3);
            
            // Generate answer from relevant chunks
            String answer = similaritySearchService.generateAnswer(question, relevantChunks);
            
            Map<String, String> response = new HashMap<>();
            response.put("answer", answer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("answer", "Error processing question: " + e.getMessage()));
        }
    }
    
    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reloadDocuments() {
        try {
            similaritySearchService.reloadDocuments();
            int chunkCount = similaritySearchService.getChunkCount();
            return ResponseEntity.ok(Map.of(
                "status", "Documents reloaded successfully",
                "chunks", String.valueOf(chunkCount)
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "Error reloading documents: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("chunksLoaded", similaritySearchService.getChunkCount());
        status.put("ready", similaritySearchService.getChunkCount() > 0);
        return ResponseEntity.ok(status);
    }
}