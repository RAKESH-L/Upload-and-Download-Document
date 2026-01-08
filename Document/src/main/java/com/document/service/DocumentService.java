package com.document.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.nio.file.Files;

import java.io.FileNotFoundException;
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

import com.document.model.Document;
import com.document.repository.DocumentRepository;

@Service
public class DocumentService {
    
    @Value("${upload.directory}")  // Make sure to define this property in your application.properties or application.yml
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Validate file name to prevent path traversal attacks
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (fileName.contains("..") || fileName.contains("/")) {
            //FIX: Throw exception if file name is invalid
            throw new IOException("Invalid file name");
        }
        //FIX: Validate file is not empty
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        //FIX: Limit file size (example: max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IOException("File size exceeds limit");
        }
        //FIX: Only allow certain file types (example: pdf, txt, docx)
        String allowedExtensions = "pdf,txt,docx";
        String ext = fileName.lastIndexOf('.') > 0 ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!allowedExtensions.contains(ext)) {
            throw new IOException("File type not allowed");
        }
        //FIX: Use Path API to resolve file path safely
        Path uploadDirPath = Paths.get(uploadDirectory).normalize();
        Path fullPath = uploadDirPath.resolve(fileName).normalize();
        if (!fullPath.startsWith(uploadDirPath)) {
            //FIX: Prevent path traversal
            throw new IOException("Invalid file path");
        }
        // Ensure the upload directory exists
        Files.createDirectories(uploadDirPath); //FIX: Use Files.createDirectories for atomic directory creation
        // Save the file to the specified directory
        file.transferTo(fullPath.toFile());
        String filePath = fullPath.toString();
        // Check if a Document with the same name already exists
        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            // Update the existing Document's file path
            Document documentToUpdate = existingDocument.get();
            documentToUpdate.setFilePath(filePath);
            documentRepository.save(documentToUpdate);
        } else {
            // Save a new Document
            Document newDocument = new Document();
            newDocument.setName(name);
            newDocument.setFilePath(filePath);
            documentRepository.save(newDocument);
        }
    }
    
    public ResponseEntity<Resource> downloadFile(Long documentId) {
        try {
            Optional<Document> optionalDocument = documentRepository.findById(documentId);
            //FIX: Remove debug prints and avoid leaking internal info
            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                String filePath = document.getFilePath();
                //FIX: Validate file path to prevent path traversal
                Path uploadDirPath = Paths.get(uploadDirectory).normalize();
                Path resolvedPath = Paths.get(filePath).normalize();
                if (!resolvedPath.startsWith(uploadDirPath)) {
                    return ResponseEntity.status(403).build(); // Forbidden
                }
                Resource resource = new UrlResource(resolvedPath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    //FIX: Set Content-Disposition header safely with quotes
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename().replaceAll("[\\r\\n]", "") + "\"");
                    headers.add("File-Downloaded", "File downloaded");
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                } else {
                    // Handle the case when the file doesn't exist
                    return ResponseEntity.notFound().build();
                }
            } else {
                // Handle the case when the document with the given ID is not found
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            //FIX: Do not print stack trace, log securely (logging omitted for brevity)
            return ResponseEntity.status(500).build(); // Internal Server Error
        }
    }
}
