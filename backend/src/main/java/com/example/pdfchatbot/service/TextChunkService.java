package com.example.pdfchatbot.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkService {
    private static final int CHUNK_SIZE = 1000; // Increased to reduce number of chunks
    private static final int OVERLAP = 200;
    
    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }
        
        // Process text in a more memory-efficient way
        // Split by sentences or paragraphs, whichever is smaller
        String normalizedText = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        
        // Split by double newlines (paragraphs) first
        String[] paragraphs = normalizedText.split("\\n\\s*\\n");
        
        StringBuilder currentChunk = new StringBuilder(CHUNK_SIZE);
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim().replaceAll("\\s+", " ");
            if (paragraph.isEmpty()) continue;
            
            // If paragraph itself is very large, split it by sentences
            if (paragraph.length() > CHUNK_SIZE * 2) {
                // First, save current chunk if it exists
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // Split large paragraph by sentences
                String[] sentences = paragraph.split("[.!?]+\\s+");
                for (String sentence : sentences) {
                    sentence = sentence.trim();
                    if (sentence.isEmpty()) continue;
                    
                    if (currentChunk.length() + sentence.length() + 1 > CHUNK_SIZE && currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString());
                        // Keep overlap from end of previous chunk
                        if (currentChunk.length() > OVERLAP) {
                            String overlap = currentChunk.substring(currentChunk.length() - OVERLAP);
                            currentChunk.setLength(0);
                            currentChunk.append(overlap).append(" ");
                        } else {
                            currentChunk.setLength(0);
                        }
                    }
                    if (currentChunk.length() > 0) {
                        currentChunk.append(" ");
                    }
                    currentChunk.append(sentence);
                }
            } else {
                // Normal paragraph processing
                if (currentChunk.length() > 0 && 
                    currentChunk.length() + paragraph.length() + 2 > CHUNK_SIZE) {
                    chunks.add(currentChunk.toString());
                    
                    // Create overlap
                    if (currentChunk.length() > OVERLAP) {
                        String overlap = currentChunk.substring(currentChunk.length() - OVERLAP);
                        currentChunk.setLength(0);
                        currentChunk.append(overlap).append("\n\n").append(paragraph);
                    } else {
                        currentChunk.setLength(0);
                        currentChunk.append(paragraph);
                    }
                } else {
                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(paragraph);
                }
            }
        }
        
        // Add remaining chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    public List<String> chunkAllTexts(List<String> texts) {
        List<String> allChunks = new ArrayList<>();
        for (String text : texts) {
            allChunks.addAll(chunkText(text));
        }
        return allChunks;
    }
}

