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

	@Autowired
    private DocumentService documentService;
	
	@PostMapping("/upload")
    public String uploadDocument(
    		@RequestParam("name") String name,
    		@RequestParam("file") MultipartFile file) {
        try {
        	documentService.uploadFile(file, name);
            return "File uploaded successfully!";
        } catch (Exception e) {
            return "Failed to upload the file: " + e.getMessage();
        }
    }
	
	@GetMapping("/d/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) throws IOException {
        return documentService.downloadFile(documentId);
    }

//    @PostMapping("/upload")
//    public ResponseEntity<Document> uploadDocument(
//            @RequestParam("name") String name,
//            @RequestParam("file") MultipartFile file) {
//
//        Document savedDocument = documentService.saveDocument(name, file);
//
//        return ResponseEntity.ok(savedDocument);
//    }
    
//    @GetMapping("/{id}")
//    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
//        Document document = documentService.getDocument(id);
//
//        return ResponseEntity.ok()
//                .header("Content-Disposition", "attachment; filename=" + document.getName())
//                .body(document.getContent());
//    }
}
