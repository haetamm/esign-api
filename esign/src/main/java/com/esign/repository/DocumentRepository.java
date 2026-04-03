package com.esign.repository;

import com.esign.model.Document;
import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String>, JpaSpecificationExecutor<Document> {
    Optional<Document> findByIdAndIsDeletedTrue(String id);
    List<Document> findAllByOwnerAndIsDeletedFalse(User owner);
}
