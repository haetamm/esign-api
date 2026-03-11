package com.esign.repository;

import com.esign.model.Folder;
import com.esign.model.FolderContributor;
import com.esign.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FolderContributorRepository extends JpaRepository<FolderContributor, String> {
    Optional<FolderContributor> findByFolderAndUser(Folder folder, User user);
    List<FolderContributor> findAllByFolder(Folder folder);

    @Modifying
    @Transactional
    @Query("DELETE FROM FolderContributor fc WHERE fc.folder = :folder AND fc.user = :user")
    void deleteByFolderAndUser(@Param("folder") Folder folder, @Param("user") User user);

    boolean existsByFolderAndUser(Folder folder, User user);
}
