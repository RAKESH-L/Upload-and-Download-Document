package com.document.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.document.model.Document;
import com.document.service.DocumentService;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    //FIX: Use constructor injection for better testability and immutability
    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("name") String name,
                                                 @RequestParam("file") MultipartFile file) {
        try {
            //FIX: Return ResponseEntity instead of String for better HTTP response control
            documentService.uploadFile(file, name);
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (Exception e) {
            //FIX: Return error as HTTP 400 instead of plain String
            return ResponseEntity.badRequest().body("Failed to upload the file: " + e.getMessage());
        }
    }

    @GetMapping("/d/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) throws IOException {
        return documentService.downloadFile(documentId);
    }

    // Legacy endpoints commented out for backward compatibility
}