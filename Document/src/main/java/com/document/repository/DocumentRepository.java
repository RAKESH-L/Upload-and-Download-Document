package com.document.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.document.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Long>{

	Optional<Document> findByName(String name);
}
