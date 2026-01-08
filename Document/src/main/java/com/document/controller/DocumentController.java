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

import com.document.service.DocumentService;

@RestController
@RequestMapping("/api/document")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("name") String name, @RequestParam("file") MultipartFile file) {
        //FIX: Input validation for document name (prevent XSS, etc.)
        if (name == null || name.trim().isEmpty() || !name.matches("^[a-zA-Z0-9_\- ]{1,100}$")) {
            return ResponseEntity.badRequest().body("Invalid document name. Only alphanumeric, space, dash, and underscore allowed, max 100 chars.");
        }
        try {
            documentService.uploadFile(file, name);
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (Exception e) {
            //FIX: Do not expose internal error details to the client
            return ResponseEntity.status(500).body("Failed to upload the file.");
        }
    }

    @GetMapping("/d/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) {
        return documentService.downloadFile(documentId);
    }
}
