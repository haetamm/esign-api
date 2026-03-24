package com.esign.repository;

import com.esign.constant.ContributorStatus;
import com.esign.model.Document;
import com.esign.model.DocumentContributor;
import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentContributorRepository extends JpaRepository<DocumentContributor, String> {
    List<DocumentContributor> findAllByDocument(Document document);

    @Query("SELECT dc.user FROM DocumentContributor dc WHERE dc.document = :document")
    List<User> findUsersByDocument(@Param("document") Document document);
}
