package com.esign.specification;

import com.esign.entities.folder.SearchFolderRequest;
import com.esign.model.Folder;
import com.esign.model.FolderContributor;
import com.esign.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FolderSpecification {

    public Specification<Folder> specification(SearchFolderRequest request, User user, List<String> userRoleIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // selalu root folder & tidak dihapus
            predicates.add(cb.isNull(root.get("parent")));
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

            // predicate area (umum atau role)
            Predicate areaPredicate;
            if (Boolean.TRUE.equals(request.getIsRole())) {
                // area role
                areaPredicate = cb.and(
                        root.get("requiredRole").get("id").in(
                                userRoleIds.isEmpty() ? List.of("") : userRoleIds
                        )
                );
            } else {
                // area umum
                areaPredicate = cb.isNull(root.get("requiredRole"));
            }
            predicates.add(areaPredicate);

            // filter mine / contributor / semua
            if ("mine".equalsIgnoreCase(request.getFilter())) {
                // hanya milik sendiri
                predicates.add(cb.equal(root.get("owner").get("id"), user.getId()));

            } else if ("contributor".equalsIgnoreCase(request.getFilter())) {
                // hanya yang jadi contributor (bukan owner)
                predicates.add(cb.and(
                        root.get("id").in(contributorSubquery),
                        cb.notEqual(root.get("owner").get("id"), user.getId())
                ));

            } else {
                // semua yang bisa diakses
                if (Boolean.TRUE.equals(request.getIsRole())) {
                    // area role → public + contributor
                    predicates.add(cb.or(
                            cb.equal(root.get("isPublic"), true),
                            root.get("id").in(contributorSubquery)
                    ));
                } else {
                    // area umum → public + milik sendiri + contributor
                    predicates.add(cb.or(
                            cb.equal(root.get("isPublic"), true),
                            cb.equal(root.get("owner").get("id"), user.getId()),
                            root.get("id").in(contributorSubquery)
                    ));
                }
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }

    private List<Predicate> commonPredicates(
            Root<Folder> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            SearchFolderRequest request,
            User user) {

        List<Predicate> predicates = new ArrayList<>();

        // tidak dihapus
        predicates.add(cb.equal(root.get("isDeleted"), false));

        // filter nama
        if (request.getName() != null && !request.getName().isBlank()) {
            predicates.add(cb.like(
                    cb.upper(root.get("name")),
                    "%" + request.getName().toUpperCase() + "%"
            ));
        }

        return predicates;
    }

    private Subquery<String> contributorSubquery(CriteriaQuery<?> query, CriteriaBuilder cb, User user) {
        Subquery<String> subquery = query.subquery(String.class);
        Root<FolderContributor> contributorRoot = subquery.from(FolderContributor.class);
        subquery.select(contributorRoot.get("folder").get("id"))
                .where(cb.equal(contributorRoot.get("user").get("id"), user.getId()));
        return subquery;
    }
}
