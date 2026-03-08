package com.esign.specification;

import com.esign.entities.role.SearchRoleRequest;
import com.esign.model.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleSpecification {

    public Specification<Role> specification(SearchRoleRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
            }

            // filter name
            if (request.getName() != null && !request.getName().isBlank()) { // ← fix disini
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("name")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            assert query != null;
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }
}