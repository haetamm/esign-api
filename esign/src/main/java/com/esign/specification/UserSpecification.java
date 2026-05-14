package com.esign.specification;

import com.esign.entities.user.SearchUserRequest;
import com.esign.model.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class UserSpecification {

    private static final Set<String> PROFILE_FIELDS = Set.of("name");

    public Specification<User> specification(SearchUserRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean needsJoin =
                    (request.getName() != null && !request.getName().isBlank()) ||
                            (request.getPhone() != null && !request.getPhone().isBlank()) ||
                            (request.getGender() != null && !request.getGender().isBlank()) ||
                            (request.getSortBy() != null && PROFILE_FIELDS.contains(request.getSortBy()));

            Join<Object, Object> profileJoin = null;
            if (needsJoin) {
                // OneToOne → tidak ada duplikat row, distinct tidak diperlukan
                profileJoin = root.join("profile", JoinType.LEFT);
            }

            if (request.getIsEnable() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isEnable"), request.getIsEnable()));
            }

            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(root.get("email")),
                        "%" + request.getEmail().toUpperCase() + "%"
                ));
            }

            if (request.getName() != null && !request.getName().isBlank()) {
                assert profileJoin != null;
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.upper(profileJoin.get("name")),
                        "%" + request.getName().toUpperCase() + "%"
                ));
            }

            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        profileJoin.get("phone"),
                        "%" + request.getPhone() + "%"
                ));
            }

            if (request.getGender() != null && !request.getGender().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(profileJoin.get("gender")),
                        request.getGender().toUpperCase()
                ));
            }

            // Sort — hanya untuk data query, bukan count query
            assert query != null;
            boolean isCountQuery = Long.class.equals(query.getResultType());
            if (!isCountQuery
                    && request.getSortBy() != null
                    && !request.getSortBy().isBlank()) {

                boolean isDesc = "desc".equalsIgnoreCase(request.getDirection());
                Order order;

                if (PROFILE_FIELDS.contains(request.getSortBy()) && profileJoin != null) {
                    order = isDesc
                            ? criteriaBuilder.desc(profileJoin.get(request.getSortBy()))
                            : criteriaBuilder.asc(profileJoin.get(request.getSortBy()));
                } else {
                    order = isDesc
                            ? criteriaBuilder.desc(root.get(request.getSortBy()))
                            : criteriaBuilder.asc(root.get(request.getSortBy()));
                }

                query.orderBy(order);
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }
}