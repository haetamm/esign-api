package com.esign.specification;

import com.esign.entities.folder.SearchSubFolderRequest;
import com.esign.model.Folder;
import com.esign.model.FolderContributor;
import com.esign.model.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SubFolderSpecification {

    public Specification<Folder> specification(SearchSubFolderRequest request, User user, Folder parent) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // selalu dalam parent ini & tidak dihapus
            predicates.add(cb.equal(root.get("parent"), parent));
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // filter nama
            if (request.getName() != null && !request.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.upper(root.get("name")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            // subquery contributor
            assert query != null;
            Subquery<String> contributorSubquery = query.subquery(String.class);
            Root<FolderContributor> contributorRoot = contributorSubquery.from(FolderContributor.class);
            contributorSubquery.select(contributorRoot.get("folder").get("id"))
                    .where(cb.equal(contributorRoot.get("user").get("id"), user.getId()));

            if ("mine".equalsIgnoreCase(request.getFilter())) {
                predicates.add(cb.equal(root.get("owner").get("id"), user.getId()));

            } else if ("contributor".equalsIgnoreCase(request.getFilter())) {
                predicates.add(cb.and(
                        root.get("id").in(contributorSubquery),
                        cb.notEqual(root.get("owner").get("id"), user.getId())
                ));

            } else {
                // semua sub folder yang bisa diakses
                predicates.add(cb.or(
                        cb.equal(root.get("isPublic"), true),
                        cb.equal(root.get("owner").get("id"), user.getId()),
                        root.get("id").in(contributorSubquery)
                ));
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }

}
