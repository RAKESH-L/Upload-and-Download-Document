package com.document.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.nio.file.Files;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.document.model.Document;
import com.document.repository.DocumentRepository;

@Service
public class DocumentService {
    //FIX: Added logger for secure logging instead of System.out
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Value("${upload.directory}")  // Make sure to define this property in your application.properties or application.yml
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Clean and validate file name to prevent path traversal
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        //FIX: Prevent directory traversal attacks
        if (fileName.contains("..")) {
            throw new IOException("Invalid file path");
        }
        //FIX: Use Path API for secure path joining
        Path uploadPath = Paths.get(uploadDirectory);
        //FIX: Use Files API for directory creation
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        //FIX: Use Path API for file path
        Path fullPath = uploadPath.resolve(fileName);
        file.transferTo(fullPath.toFile());

        // Check if a Document with the same name already exists
        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            Document documentToUpdate = existingDocument.get();
            //FIX: Store canonical path
            documentToUpdate.setFilePath(fullPath.toString());
            documentRepository.save(documentToUpdate);
        } else {
            Document newDocument = new Document();
            newDocument.setName(name);
            //FIX: Store canonical path
            newDocument.setFilePath(fullPath.toString());
            documentRepository.save(newDocument);
        }
    }

    public ResponseEntity<Resource> downloadFile(Long documentId) {
        //FIX: Removed verbose System.out and added logger
        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isPresent()) {
            Document document = optionalDocument.get();
            //FIX: Use Path API for file path
            Path filePath = Paths.get(document.getFilePath());
            try {
                //FIX: Check resource is readable
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename());
                    headers.add("File-Downloaded", "File downloaded");
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                } else {
                    logger.warn("File not found or not readable: {}", filePath);
                    return ResponseEntity.notFound().build();
                }
            } catch (IOException e) {
                logger.error("Error downloading file: {}", filePath, e);
                return ResponseEntity.status(500).build(); // Internal Server Error
            }
        } else {
            logger.warn("Document not found with ID: {}", documentId);
            return ResponseEntity.notFound().build();
        }
    }
}
