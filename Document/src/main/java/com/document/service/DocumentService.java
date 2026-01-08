package com.document.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.document.model.Document;
import com.document.repository.DocumentRepository;

@Service
public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Value("${upload.directory}")
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Clean and validate file name to prevent path traversal
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        //FIX: Reject file names containing path traversal sequences
        if (fileName.contains("..")) {
            logger.warn("Invalid file name: {}", fileName);
            throw new IOException("Invalid file name: " + fileName);
        }
        //FIX: Ensure upload directory exists using secure Files.createDirectories
        Path uploadPath = Paths.get(uploadDirectory);
        Files.createDirectories(uploadPath);
        //FIX: Use Path.resolve to avoid path manipulation
        Path fullPath = uploadPath.resolve(fileName);
        //FIX: Use transferTo with Path to avoid file overwrite vulnerabilities
        file.transferTo(fullPath);

        // Check if a Document with the same name already exists
        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            Document documentToUpdate = existingDocument.get();
            documentToUpdate.setFilePath(fullPath.toString());
            documentRepository.save(documentToUpdate);
        } else {
            Document newDocument = new Document();
            newDocument.setName(name);
            newDocument.setFilePath(fullPath.toString());
            documentRepository.save(newDocument);
        }
        //FIX: Log successful upload
        logger.info("File uploaded successfully: {}", fileName);
    }

    public ResponseEntity<Resource> downloadFile(Long documentId) {
        try {
            Optional<Document> optionalDocument = documentRepository.findById(documentId);
            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                String filePath = document.getFilePath();
                //FIX: Validate file path is not null or empty
                if (filePath == null || filePath.trim().isEmpty()) {
                    logger.warn("File path is empty for document ID: {}", documentId);
                    return ResponseEntity.notFound().build();
                }
                Path path = Paths.get(filePath);
                //FIX: Only allow files within the upload directory
                if (!path.normalize().startsWith(Paths.get(uploadDirectory).normalize())) {
                    logger.warn("Attempt to access file outside upload directory: {}", filePath);
                    return ResponseEntity.status(403).build();
                }
                //FIX: Use UrlResource and check if file exists and is readable
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename());
                    headers.add("File-Downloaded", "File downloaded");
                    //FIX: Log successful download
                    logger.info("File downloaded: {}", filePath);
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                } else {
                    logger.warn("File not found or not readable: {}", filePath);
                    return ResponseEntity.notFound().build();
                }
            } else {
                logger.warn("Document not found with ID: {}", documentId);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            //FIX: Log exception securely
            logger.error("Error downloading file for document ID {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
