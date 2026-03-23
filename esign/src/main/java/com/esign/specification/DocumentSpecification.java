package com.esign.specification;

import com.esign.entities.folder.SearchFolderRequest;
import com.esign.entities.folder.SearchSubFolderRequest;
import com.esign.model.Document;
import com.esign.model.DocumentContributor;
import com.esign.model.Folder;
import com.esign.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentSpecification {

    public Specification<Document> rootSpecification(
            SearchFolderRequest request, User user, List<String> userRoleIds) {

        return Specification.<Document>unrestricted()
                .and(notDeleted())
                .and(isRootDocument())
                .and(titleContains(request.getName()))
                .and(roleFilter(request.getIsRole(), userRoleIds))
                .and(accessFilter(request.getFilter(), user));
    }

    public Specification<Document> folderSpecification(
            SearchSubFolderRequest request, User user, Folder folder) {

        return Specification.<Document>unrestricted()
                .and(notDeleted())
                .and(inFolder(folder))
                .and(titleContains(request.getName()))
                .and(accessFilter(request.getFilter(), user));
    }


    // dokumen yang belum dihapus
    private Specification<Document> notDeleted() {
        return (root, query, cb) -> cb.equal(root.get("isDeleted"), false);
    }

    // dokumen di root (tidak punya folder)
    private Specification<Document> isRootDocument() {
        return (root, query, cb) -> cb.isNull(root.get("folder"));
    }

    // dokumen di dalam folder
    private Specification<Document> inFolder(Folder folder) {
        return (root, query, cb) -> cb.equal(root.get("folder"), folder);
    }

    // Filter judul dokumen (case-insensitive, partial match)
    private Specification<Document> titleContains(String name) {
        if (name == null || name.isBlank()) return null;
        return (root, query, cb) -> cb.like(
                cb.upper(root.get("title")),
                "%" + name.toUpperCase() + "%"
        );
    }

    // isRole => true = untuk role tertentu | isRole => false = untuk semua orang
    private Specification<Document> roleFilter(Boolean isRole, List<String> userRoleIds) {
        if (Boolean.TRUE.equals(isRole)) {
            List<String> safeRoleIds = userRoleIds.isEmpty() ? List.of("") : userRoleIds;
            return (root, query, cb) -> root.get("requiredRole").get("id").in(safeRoleIds);
        } else {
            return (root, query, cb) -> cb.isNull(root.get("requiredRole"));
        }
    }

    // filter mine / contributor / semua
    private Specification<Document> accessFilter(String filter, User user) {
        if ("mine".equalsIgnoreCase(filter)) {
            // hanya milik sendiri
            return isOwnedBy(user);

        } else if ("contributor".equalsIgnoreCase(filter)) {
            // hanya yang jadi contributor (penandatangan)
            return isContributorOf(user).and(not(isOwnedBy(user)));

        } else {
            // semua yang bisa diakses
            return null;
        }
    }

    // dimiliki (di-upload) oleh user
    private Specification<Document> isOwnedBy(User user) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), user.getId());
    }

    // Dokumen yang user ini terdaftar sebagai penandatangan (contributor)
    private Specification<Document> isContributorOf(User user) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<String> subquery = query.subquery(String.class);
            Root<DocumentContributor> contributor = subquery.from(DocumentContributor.class);
            subquery
                    .select(contributor.get("document").get("id"))
                    .where(cb.equal(contributor.get("user").get("id"), user.getId()));
            return root.get("id").in(subquery);
        };
    }

    private static <T> Specification<T> not(Specification<T> spec) {
        return (root, query, cb) -> cb.not(spec.toPredicate(root, query, cb));
    }
}