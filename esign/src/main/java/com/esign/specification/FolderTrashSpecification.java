package com.esign.specification;

import com.esign.entities.folder.SearchFolderTrashRequest;
import com.esign.model.Folder;
import com.esign.model.FolderContributor;
import com.esign.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FolderTrashSpecification {

    public Specification<Folder> specification(SearchFolderTrashRequest request, User user, List<String> userRoleIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), true));

            if (request.getName() != null && !request.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.upper(root.get("name")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            assert query != null;
            Subquery<String> contribSubquery = query.subquery(String.class);
            Root<FolderContributor> contribRoot = contribSubquery.from(FolderContributor.class);
            contribSubquery.select(contribRoot.get("folder").get("id"))
                    .where(cb.equal(contribRoot.get("user").get("id"), user.getId()));

            if ("role".equalsIgnoreCase(request.getType())) {
                predicates.add(
                        root.get("requiredRole").get("id").in(
                                userRoleIds.isEmpty() ? List.of("") : userRoleIds
                        )
                );
            } else if ("public".equalsIgnoreCase(request.getType())) {
                predicates.add(cb.isNull(root.get("requiredRole")));
            } else {
                predicates.add(cb.or(
                        cb.isNull(root.get("requiredRole")),
                        root.get("requiredRole").get("id").in(
                                userRoleIds.isEmpty() ? List.of("") : userRoleIds
                        ),
                        root.get("id").in(contribSubquery)
                ));
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }
}
