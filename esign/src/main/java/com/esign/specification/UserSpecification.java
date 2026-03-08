package com.esign.specification;

import com.esign.entities.user.SearchUserRequest;
import com.esign.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserSpecification {

    public Specification<User> specification(SearchUserRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getIsEnable() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isEnable"), request.getIsEnable()));
            }

            // filter email
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("email")),
                        "%" + request.getEmail().toUpperCase() + "%"
                ));
            }

            // filter name, phone, gender → dari profile
            if (request.getName() != null && !request.getName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.join("profile").get("name")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        root.join("profile").get("phone"),
                        "%" + request.getPhone() + "%"
                ));
            }

            if (request.getGender() != null && !request.getGender().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.join("profile").get("gender")),
                        request.getGender().toUpperCase()
                ));
            }

            assert query != null;
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }
}