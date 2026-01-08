package com.document.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.document.model.Document;
import com.document.repository.DocumentRepository;

@Service
public class DocumentService {

    //FIX: Added logger for secure exception and event logging
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Value("${upload.directory}")
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Uploads a file and saves or updates the corresponding Document entity.
     * @param file the file to upload
     * @param name the document name
     * @throws IOException if file operations fail
     */
    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Validate and sanitize file name to prevent path traversal
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            //FIX: Block path traversal attempts
            throw new IOException("Invalid file name: " + originalFileName);
        }

        //FIX: Use normalized absolute path for upload directory
        Path uploadDirPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirPath);

        //FIX: Use resolved absolute path for file storage
        Path targetLocation = uploadDirPath.resolve(originalFileName);
        file.transferTo(targetLocation);

        String filePath = targetLocation.toString();

        Optional<Document> existingDocument = documentRepository.findByName(name);
        if (existingDocument.isPresent()) {
            Document documentToUpdate = existingDocument.get();
            documentToUpdate.setFilePath(filePath);
            documentRepository.save(documentToUpdate);
            //FIX: Log update event
            logger.info("Updated existing document: {}", documentToUpdate);
        } else {
            Document newDocument = new Document();
            newDocument.setName(name);
            newDocument.setFilePath(filePath);
            documentRepository.save(newDocument);
            //FIX: Log new document event
            logger.info("Saved new document: {}", newDocument);
        }
    }

    /**
     * Downloads a file for the given document ID.
     * @param documentId the document ID
     * @return ResponseEntity with the file resource or error status
     */
    public ResponseEntity<Resource> downloadFile(Long documentId) {
        try {
            Optional<Document> optionalDocument = documentRepository.findById(documentId);

            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                String filePath = document.getFilePath();

                Path path;
                try {
                    //FIX: Validate file path
                    path = Paths.get(filePath).toAbsolutePath().normalize();
                } catch (InvalidPathException e) {
                    //FIX: Log invalid path and return bad request
                    logger.error("Invalid file path: {}", filePath, e);
                    return ResponseEntity.badRequest().build();
                }

                Resource resource = new UrlResource(path.toUri());
                //FIX: Check if file exists and is readable
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    //FIX: Set Content-Disposition header with safe filename
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"");
                    headers.add("File-Downloaded", "File downloaded");

                    logger.info("File downloaded: {}", filePath);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                } else {
                    //FIX: Log missing or unreadable file
                    logger.warn("File not found or not readable: {}", filePath);
                    return ResponseEntity.notFound().build();
                }
            } else {
                //FIX: Log missing document
                logger.warn("Document not found with ID: {}", documentId);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            //FIX: Log exception securely
            logger.error("Error downloading file for document ID: {}", documentId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
