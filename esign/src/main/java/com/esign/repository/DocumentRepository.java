package com.esign.repository;

import com.esign.model.Document;
import com.esign.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByIdAndIsDeletedFalse(String id);
}
