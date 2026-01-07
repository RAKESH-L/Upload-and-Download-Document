package com.document.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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
        String originalFileName = file.getOriginalFilename();
        String fileName = StringUtils.cleanPath(originalFileName);
        if (fileName.contains("..")) {
            //FIX: Reject files with invalid path sequences
            throw new IOException("Invalid file name: " + fileName);
        }
        //FIX: Generate a unique filename to prevent overwriting and enumeration
        String fileExtension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = fileName.substring(dotIndex);
        }
        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        String filePath = uploadDirectory + "/" + storedFileName;

        // Ensure the upload directory exists
        File directory = new File(uploadDirectory);
        if (!directory.exists()) {
            //FIX: Use mkdirs return value to check for directory creation failure
            if (!directory.mkdirs()) {
                throw new IOException("Failed to create upload directory");
            }
        }

        // Save the file to the specified directory
        Path fullPath = Paths.get(uploadDirectory, storedFileName);
        //FIX: Use Files.copy with REPLACE_EXISTING and check for IOExceptions
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        // Check if a Document with the same name already exists
        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            // Update the existing Document's file path
            Document documentToUpdate = existingDocument.get();
            //FIX: Store only the storedFileName, not the original file name or user input
            documentToUpdate.setFilePath(filePath);
            documentRepository.save(documentToUpdate);
        } else {
            // Save a new Document
            Document newDocument = new Document();
            newDocument.setName(name);
            //FIX: Store only the storedFileName, not the original file name or user input
            newDocument.setFilePath(filePath);
            documentRepository.save(newDocument);
        }
    }

    public ResponseEntity<Resource> downloadFile(Long documentId) {
        try {
            Optional<Document> optionalDocument = documentRepository.findById(documentId);
            //FIX: Remove debug print statements to avoid information leakage
            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                String filePath = document.getFilePath();
                //FIX: Validate file path to prevent path traversal
                Path safePath = Paths.get(uploadDirectory).resolve(Paths.get(filePath).getFileName()).normalize();
                Resource resource = new UrlResource(safePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    //FIX: Encode filename for Content-Disposition header to prevent header injection
                    String encodedFileName = java.net.URLEncoder.encode(resource.getFilename(), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
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
            //FIX: Log the exception securely (use a logger in production)
            // e.printStackTrace();
            return ResponseEntity.status(500).build(); // Internal Server Error
        }
    }
}
