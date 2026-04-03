package com.esign.repository;

import com.esign.model.Document;
import com.esign.model.DocumentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentActivityRepository extends JpaRepository<DocumentActivity, String> {
    List<DocumentActivity> findAllByDocumentInAndCreatedAtAfterOrderByCreatedAtDesc(
            List<Document> documents,
            LocalDateTime after
    );
}
