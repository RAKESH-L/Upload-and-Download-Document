package com.document.service;

import com.document.model.Document;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DocumentService {
    //FIX: Use thread-safe list for concurrent access
    private final List<Document> documents = new CopyOnWriteArrayList<>();

    public List<Document> getAllDocuments() {
        //FIX: Return an unmodifiable list to prevent external modification
        return Collections.unmodifiableList(documents);
    }

    public Optional<Document> getDocumentById(Long id) {
        return documents.stream().filter(doc -> doc.getId().equals(id)).findFirst();
    }

    public Document saveDocument(Document document) {
        //FIX: Validate document fields to prevent null or malicious input
        if (document == null || document.getId() == null || document.getName() == null || document.getContent() == null) {
            throw new IllegalArgumentException("Invalid document data");
        }
        documents.add(document);
        return document;
    }

    public boolean deleteDocument(Long id) {
        return documents.removeIf(doc -> doc.getId().equals(id));
    }
}
