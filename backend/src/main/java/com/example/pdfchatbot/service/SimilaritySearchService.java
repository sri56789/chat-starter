package com.example.pdfchatbot.service;

import org.apache.commons.text.similarity.CosineDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimilaritySearchService {
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private TextChunkService textChunkService;
    
    @Autowired
    private LlmService llmService;
    
    private List<String> textChunks = new ArrayList<>();
    private final CosineDistance cosineDistance = new CosineDistance();
    
    @PostConstruct
    public void initialize() {
        // Don't load PDFs on startup to avoid OutOfMemoryError
        // PDFs will be loaded when user clicks "Reload PDFs" button
        System.out.println("SimilaritySearchService initialized. Use /api/reload to load PDFs.");
    }
    
    public void reloadDocuments() throws IOException {
        System.out.println("Reloading PDF documents...");
        System.out.println("Memory before: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + " MB used");
        
        // Process PDFs one at a time to avoid memory issues
        List<String> allChunks = new ArrayList<>();
        List<String> pdfTexts = pdfService.extractTextFromAllPdfs();
        
        if (pdfTexts.isEmpty()) {
            System.err.println("WARNING: No PDF text extracted. Check if PDFs exist in the pdfs folder.");
            textChunks = allChunks;
            return;
        }
        
        System.out.println("Extracted text from " + pdfTexts.size() + " PDF(s)");
        
        // Process each PDF separately to reduce memory footprint
        for (int i = 0; i < pdfTexts.size(); i++) {
            String pdfText = pdfTexts.get(i);
            System.out.println("Chunking PDF " + (i + 1) + " of " + pdfTexts.size() + " (size: " + pdfText.length() + " chars)");
            List<String> chunks = textChunkService.chunkText(pdfText);
            allChunks.addAll(chunks);
            System.out.println("Created " + chunks.size() + " chunks from PDF " + (i + 1));
            
            // Suggest GC after processing each PDF
            if (i < pdfTexts.size() - 1) {
                System.gc();
            }
        }
        
        textChunks = allChunks;
        System.out.println("Loaded " + textChunks.size() + " text chunks from PDFs");
        System.out.println("Memory after: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + " MB used");
    }
    
    public List<String> findMostRelevantChunks(String query, int topK) {
        if (textChunks.isEmpty() || query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // Simple keyword-based scoring
        String queryLower = query.toLowerCase().trim();
        String[] queryWords = queryLower.split("\\s+");
        
        List<ChunkScore> scoredChunks = new ArrayList<>();
        
        for (int i = 0; i < textChunks.size(); i++) {
            String chunk = textChunks.get(i);
            String chunkLower = chunk.toLowerCase();
            
            double score = 0.0;
            
            // Count word matches
            for (String word : queryWords) {
                if (word.length() > 2) { // Ignore very short words
                    int count = countOccurrences(chunkLower, word);
                    score += count;
                    
                    // Bonus for exact phrase match
                    if (chunkLower.contains(queryLower)) {
                        score += 5.0;
                    }
                }
            }
            
            // Cosine similarity (simple character-based)
            try {
                int compareLength = Math.min(chunkLower.length(), Math.max(queryLower.length() * 2, 100));
                if (compareLength > 0 && chunkLower.length() >= compareLength) {
                    double similarity = 1.0 - cosineDistance.apply(
                        normalizeText(queryLower),
                        normalizeText(chunkLower.substring(0, compareLength))
                    );
                    score += similarity * 2.0;
                }
            } catch (Exception e) {
                // If similarity calculation fails, continue with keyword-based scoring
            }
            
            scoredChunks.add(new ChunkScore(i, score));
        }
        
        // Sort by score descending and return top K
        return scoredChunks.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(cs -> textChunks.get(cs.index))
                .collect(Collectors.toList());
    }
    
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
    
    private String normalizeText(String text) {
        return text.replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }
    
    private static class ChunkScore {
        int index;
        double score;
        
        ChunkScore(int index, double score) {
            this.index = index;
            this.score = score;
        }
    }
    
    public int getChunkCount() {
        return textChunks.size();
    }
    
    public String generateAnswer(String question, List<String> relevantChunks) {
        if (relevantChunks.isEmpty()) {
            if (textChunks.isEmpty()) {
                return "I couldn't find relevant information in the PDFs to answer your question. Please make sure you have uploaded PDF files in the pdfs folder and click 'Reload PDFs'.";
            } else {
                return "I couldn't find relevant information in the PDFs to answer your question. Try rephrasing your question or asking about a different topic.";
            }
        }
        
        // Use LLM to generate answer from relevant chunks
        return llmService.generateAnswer(question, relevantChunks);
    }
}

