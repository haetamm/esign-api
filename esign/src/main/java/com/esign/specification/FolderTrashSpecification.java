package com.esign.specification;

import com.esign.entities.folder.SearchTrashRequest;
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

    public Specification<Folder> specification(SearchTrashRequest request, User user, List<String> userRoleIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), true));
            predicates.add(cb.equal(root.get("isDirectDeleted"), true));

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
                predicates.add(cb.or(
                        // 1. role sama & isPublic=true
                        cb.and(
                                root.get("requiredRole").get("id").in(userRoleIds),
                                cb.equal(root.get("isPublic"), true)
                        ),
                        // 2. role sama & isPublic=false & contributor BUKAN owner
                        cb.and(
                                root.get("requiredRole").get("id").in(userRoleIds),
                                cb.equal(root.get("isPublic"), false),
                                root.get("id").in(contribSubquery),
                                cb.notEqual(root.get("owner").get("id"), user.getId()) // ← bukan owner
                        ),
                        // 3. folder role (sama) & owner (cek role karena owner bisa ganti role)
                        cb.and(
                                root.get("requiredRole").get("id").in(userRoleIds),
                                cb.equal(root.get("owner").get("id"), user.getId())
                        )
                ));

            } else if ("public".equalsIgnoreCase(request.getType())) {
                predicates.add(cb.or(
                        // 1. folder umum & isPublic=true → semua user bisa lihat
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.get("isPublic"), true)
                        ),
                        // 2. folder umum & isPublic=false & user terdaftar sebagai contributor
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.get("isPublic"), false),
                                root.get("id").in(contribSubquery)
                        )
                ));
            } else {
                predicates.add(cb.or(
                        // 1. folder umum & isPublic=true → semua user bisa lihat
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.get("isPublic"), true)
                        ),
                        // 2. folder role & isPublic=true & role sesuai user
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.get("isPublic"), true)
                        ),
                        // 3. folder umum & isPublic=false & contributor
                        //    (termasuk owner folder umum)
                        cb.and(
                                cb.isNull(root.get("requiredRole")),
                                cb.equal(root.get("isPublic"), false),
                                root.get("id").in(contribSubquery)
                        ),
                        // 4. folder role & isPublic=false & contributor & role sesuai user
                        //    (contributor biasa yg masih punya role sama)
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.get("isPublic"), false),
                                root.get("id").in(contribSubquery)
                        ),
                        // 5. owner folder role & role sesuai user
                        //    (owner yang rolenya sudah berbeda tidak bisa lihat)
                        cb.and(
                                root.get("requiredRole").get("id").in(
                                        userRoleIds.isEmpty() ? List.of("") : userRoleIds
                                ),
                                cb.equal(root.get("owner").get("id"), user.getId())
                        )
                ));
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }

}