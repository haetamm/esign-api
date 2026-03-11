package com.esign.repository;

import com.esign.model.Folder;
import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, String>, JpaSpecificationExecutor<Folder> {
    List<Folder> findAllByOwnerAndParentIsNullAndIsDeletedFalse(User owner);
    List<Folder> findAllByParentAndIsDeletedFalse(Folder parent);
    boolean existsByNameAndParentAndIsDeletedFalse(String name, Folder parent);
    Optional<Folder> findByIdAndIsDeletedFalse(String id);
}
