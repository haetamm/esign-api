package com.esign.repository;

import com.esign.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, String>, JpaSpecificationExecutor<Folder> {
    List<Folder> findAllByParentAndIsDeletedFalse(Folder parent);
    List<Folder> findAllByOriginalParentIdAndIsDeletedTrue(String originalParentId);
    boolean existsByNameAndParentAndIsDeletedFalse(String name, Folder parent);
    Optional<Folder> findByIdAndIsDeletedFalse(String id);
    Optional<Folder> findByIdAndIsDeletedTrue(String id);
}
