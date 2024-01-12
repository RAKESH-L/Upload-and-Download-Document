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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.InputStreamResource;
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
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String filePath = uploadDirectory + "/" + fileName;

        // Ensure the upload directory exists
        File directory = new File(uploadDirectory);
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }

        // Save the file to the specified directory
        Path fullPath = Paths.get(uploadDirectory, fileName);
        file.transferTo(fullPath.toFile());

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
            System.out.println("opd"+optionalDocument);

            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                System.out.println("d"+document);

                String filePath = document.getFilePath();
                System.out.println("fp"+filePath);

                Resource resource = new UrlResource(Paths.get(filePath).toUri());
                System.out.println("r"+resource);

                if (resource.exists()) {
                    HttpHeaders headers = new HttpHeaders();
                    System.out.println("h"+headers);
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename());
                 // Set a custom message in the response headers
                    headers.add("File-Downloaded", "File downloaded");
                    
                    System.out.println("h2"+headers);
                    System.out.println("done");
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
            // Log the exception
            e.printStackTrace();
            return ResponseEntity.status(500).build(); // Internal Server Error
        }
    }

//	
//	public Document saveDocument(String name, MultipartFile file) {
//        try {
//            Document document = new Document();
//            document.setName(name);
//            document.setContent(file.getBytes());
//            return documentRepository.save(document);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to save document", e);
//        }
//    }
//	
//	public Document getDocument(Long id) {
//        return documentRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Document not found"));
//    }
    
}
