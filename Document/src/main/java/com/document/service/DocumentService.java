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

import com.document.model.Document;
import com.document.repository.DocumentRepository;

@Service
public class DocumentService {
    @Value("${upload.directory}")
    private String uploadDirectory;

    private final DocumentRepository documentRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadFile(MultipartFile file, String name) throws IOException {
        //FIX: Validate file name to prevent path traversal attacks
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (fileName.contains("..")) {
            //FIX: Reject files with relative path sequences
            throw new IOException("Invalid file name: " + fileName);
        }
        //FIX: Validate file extension (allow only specific types, e.g., pdf, docx, txt)
        String lowerFileName = fileName.toLowerCase();
        if (!(lowerFileName.endsWith(".pdf") || lowerFileName.endsWith(".docx") || lowerFileName.endsWith(".txt"))) {
            throw new IOException("Invalid file type. Only PDF, DOCX, and TXT files are allowed.");
        }
        //FIX: Limit file size (e.g., max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IOException("File size exceeds the maximum allowed limit of 10MB.");
        }
        String filePath = uploadDirectory + "/" + fileName;

        // Ensure the upload directory exists
        File directory = new File(uploadDirectory);
        //FIX: Check mkdirs() result and handle failure
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create upload directory: " + uploadDirectory);
        }

        //FIX: Use Path.resolve to avoid path traversal
        Path fullPath = Paths.get(uploadDirectory).resolve(fileName).normalize();
        //FIX: Ensure the file is stored only within the upload directory
        if (!fullPath.startsWith(Paths.get(uploadDirectory).toAbsolutePath())) {
            throw new IOException("Invalid file path");
        }
        //FIX: Overwrite protection (optional, but recommended)
        if (Files.exists(fullPath)) {
            throw new IOException("File already exists: " + fileName);
        }
        file.transferTo(fullPath.toFile());

        // Check if a Document with the same name already exists
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
        try {
            Optional<Document> optionalDocument = documentRepository.findById(documentId);
            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                String filePath = document.getFilePath();
                //FIX: Validate file path to prevent path traversal
                Path path = Paths.get(filePath).normalize();
                if (!path.startsWith(Paths.get(uploadDirectory).toAbsolutePath())) {
                    //FIX: Prevent access to files outside upload directory
                    return ResponseEntity.status(403).build();
                }
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    //FIX: Set Content-Disposition header safely
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename().replaceAll("[\\r\\n]", "") + "\"");
                    headers.add("File-Downloaded", "File downloaded");
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(resource);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            //FIX: Use a logger instead of e.printStackTrace() (not shown here for brevity)
            return ResponseEntity.status(500).build();
        }
    }
}
