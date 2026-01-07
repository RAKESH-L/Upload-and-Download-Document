package com.document.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.nio.file.Files;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.document.model.Document;
import com.document.repository.DocumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DocumentService {
    //FIX: Added SLF4J logger for secure logging
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Value("${upload.directory}")  // Make sure to define this property in your application.properties or application.yml
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    //FIX: Removed @Autowired from constructor for constructor injection best practice
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Clean file name and prevent path traversal
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        //FIX: Validate file name to prevent directory traversal attacks
        if (fileName.contains("..")) {
            throw new IOException("Invalid file path: " + fileName);
        }
        //FIX: Use Path API for directory creation and file path resolution
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); //FIX: Use Files API for atomic directory creation
        }
        Path fullPath = uploadPath.resolve(fileName);
        file.transferTo(fullPath);
        String filePath = fullPath.toString();
        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            Document documentToUpdate = existingDocument.get();
            documentToUpdate.setFilePath(filePath);
            documentRepository.save(documentToUpdate);
        } else {
            Document newDocument = new Document();
            newDocument.setName(name);
            newDocument.setFilePath(filePath);
            documentRepository.save(newDocument);
        }
    }

    public ResponseEntity<Resource> downloadFile(Long documentId) {
        //FIX: Removed debug prints, use logger for secure logging
        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty()) {
            logger.warn("Document with ID {} not found", documentId);
            return ResponseEntity.notFound().build();
        }
        Document document = optionalDocument.get();
        String filePath = document.getFilePath();
        try {
            //FIX: Validate file existence before loading
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("File not found at path: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(path.toUri());
            //FIX: Check resource readability
            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("Resource not readable at path: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            //FIX: Set Content-Disposition header with quoted filename to prevent header injection
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
            headers.add("File-Downloaded", "File downloaded");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            //FIX: Use logger for exception logging
            logger.error("Error while downloading file with ID {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
