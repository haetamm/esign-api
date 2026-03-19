package com.esign.repository;

import com.esign.constant.ContributorStatus;
import com.esign.model.Document;
import com.esign.model.DocumentContributor;
import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentContributorRepository extends JpaRepository<DocumentContributor, String> {
    List<DocumentContributor> findAllByDocument(Document document);
    boolean existsByDocumentAndUser(Document document, User user);
    boolean existsByDocumentAndStatusNot(Document document, ContributorStatus status);
}
