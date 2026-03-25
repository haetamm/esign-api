package com.esign.specification;

import com.esign.entities.folder.SearchTrashRequest;
import com.esign.model.Document;
import com.esign.model.DocumentContributor;
import com.esign.model.FolderContributor;
import com.esign.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class DocumentTrashSpecification {

    public Specification<Document> specification(SearchTrashRequest request, User user, List<String> userRoleIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), true));

            if (request.getName() != null && !request.getName().isBlank()) {
                predicates.add(cb.like(
                        cb.upper(root.get("title")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            assert query != null;

            // subquery contributor penandatangan
            Subquery<String> signerSubquery = query.subquery(String.class);
            Root<DocumentContributor> signerRoot = signerSubquery.from(DocumentContributor.class);
            signerSubquery.select(signerRoot.get("document").get("id"))
                    .where(cb.equal(signerRoot.get("user").get("id"), user.getId()));

            // subquery contributor folder (MANAGE/UPLOAD)
            Subquery<String> folderContribSubquery = query.subquery(String.class);
            Root<FolderContributor> folderContribRoot = folderContribSubquery.from(FolderContributor.class);
            folderContribSubquery.select(folderContribRoot.get("folder").get("id"))
                    .where(cb.equal(folderContribRoot.get("user").get("id"), user.getId()));

            if ("role".equalsIgnoreCase(request.getType())) {
                predicates.add(cb.or(
                        // 1. role sama & folder isPublic=true
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.join("folder").get("isPublic"), true)
                        ),
                        // 2. role sama & folder isPublic=false & user contributor folder
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.join("folder").get("isPublic"), false),
                                root.get("folder").get("id").in(folderContribSubquery)
                        ),
                        // 3. role sama & owner document
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.get("owner").get("id"), user.getId())
                        ),
                        // 4. role sama & user adalah signer
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                root.get("id").in(signerSubquery)
                        )
                ));

            } else if ("public".equalsIgnoreCase(request.getType())) {
                predicates.add(cb.or(
                        // 1. document umum & tanpa folder (root)
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.isNull(root.get("folder"))
                        ),
                        // 2. document umum & folder isPublic=true
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.join("folder").get("isPublic"), true)
                        ),
                        // 3. document umum & folder isPublic=false & contributor folder
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.join("folder").get("isPublic"), false),
                                root.get("folder").get("id").in(folderContribSubquery)
                        ),
                        // 4. document umum & owner document
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.get("owner").get("id"), user.getId())
                        ),
                        // 5. document umum & user adalah signer
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                root.get("id").in(signerSubquery)
                        )
                ));

            } else {
                predicates.add(cb.or(
                        // 1. document umum & tanpa folder (root)
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.isNull(root.get("folder"))
                        ),
                        // 2. document umum & folder isPublic=true
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.join("folder").get("isPublic"), true)
                        ),
                        // 3. document umum & folder isPublic=false & contributor folder
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.join("folder").get("isPublic"), false),
                                root.get("folder").get("id").in(folderContribSubquery)
                        ),
                        // 4. document role & folder isPublic=true & role sesuai user
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.join("folder").get("isPublic"), true)
                        ),
                        // 5. document role & folder isPublic=false & contributor folder & role sesuai
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.join("folder").get("isPublic"), false),
                                root.get("folder").get("id").in(folderContribSubquery)
                        ),
                        // 6. owner document (umum & role sesuai)
                        cb.and(
                                cb.equal(root.get("owner").get("id"), user.getId()),
                                cb.or(
                                        cb.isNull(root.get("requiredRole")),
                                        root.get("requiredRole").get("id").in(
                                                userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                        )
                                )
                        ),
                        // 7. user adalah signer (umum & role sesuai)
                        cb.and(
                                root.get("id").in(signerSubquery),
                                cb.or(
                                        cb.isNull(root.get("requiredRole")),
                                        root.get("requiredRole").get("id").in(
                                                userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                        )
                                )
                        )
                ));
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }
}
