package com.example.pdfchatbot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PdfService {
    private String pdfDirectory;
    
    public PdfService() {
        // Try multiple possible paths
        String[] possiblePaths = {
            System.getProperty("user.dir") + "/pdfs",                    // From project root
            System.getProperty("user.dir") + "/../pdfs",                 // From backend folder
            System.getProperty("user.dir") + "/backend/pdfs",            // Alternative
            "pdfs",                                                       // Relative
            "../pdfs"                                                     // Relative from backend
        };
        
        for (String path : possiblePaths) {
            try {
                Path testPath = Paths.get(path).toAbsolutePath().normalize();
                if (Files.exists(testPath) && Files.isDirectory(testPath)) {
                    pdfDirectory = testPath.toString();
                    System.out.println("PDF directory found at: " + pdfDirectory);
                    try (Stream<Path> paths = Files.list(testPath)) {
                        long pdfCount = paths
                            .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                            .count();
                        System.out.println("Found " + pdfCount + " PDF file(s) in directory");
                    } catch (IOException e) {
                        System.err.println("Error listing PDF files: " + e.getMessage());
                    }
                    return;
                }
            } catch (Exception e) {
                // Continue to next path
                continue;
            }
        }
        
        // Default fallback
        pdfDirectory = Paths.get(System.getProperty("user.dir"), "pdfs").toAbsolutePath().normalize().toString();
        System.err.println("Warning: PDF directory not found. Using default: " + pdfDirectory);
        System.err.println("Current working directory: " + System.getProperty("user.dir"));
    }
    
    public List<String> extractTextFromAllPdfs() throws IOException {
        List<String> allText = new ArrayList<>();
        Path pdfPath = Paths.get(pdfDirectory);
        
        if (!Files.exists(pdfPath)) {
            System.err.println("PDF directory does not exist: " + pdfDirectory);
            System.err.println("Current working directory: " + System.getProperty("user.dir"));
            return allText;
        }
        
        try (Stream<Path> paths = Files.walk(pdfPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                 .forEach(pdfFile -> {
                     try {
                         System.out.println("Processing PDF: " + pdfFile.getFileName());
                         String text = extractTextFromPdf(pdfFile.toFile());
                         if (text != null && !text.trim().isEmpty()) {
                             allText.add(text);
                             System.out.println("Successfully extracted " + text.length() + " characters from " + pdfFile.getFileName());
                         } else {
                             System.out.println("Warning: No text extracted from " + pdfFile.getFileName());
                         }
                     } catch (IOException e) {
                         System.err.println("Error reading PDF: " + pdfFile + " - " + e.getMessage());
                         e.printStackTrace();
                     }
                 });
        }
        
        System.out.println("Total PDFs processed: " + allText.size());
        return allText;
    }
    
    public String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    public List<File> getAllPdfFiles() throws IOException {
        List<File> pdfFiles = new ArrayList<>();
        Path pdfPath = Paths.get(pdfDirectory);
        
        if (!Files.exists(pdfPath)) {
            return pdfFiles;
        }
        
        try (Stream<Path> paths = Files.walk(pdfPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                 .forEach(pdfFile -> pdfFiles.add(pdfFile.toFile()));
        }
        
        return pdfFiles;
    }
}

