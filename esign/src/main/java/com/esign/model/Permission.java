package com.esign.model;

import com.esign.constant.ActionType;
import com.esign.constant.TableName;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = TableName.T_PERMISSION,
        uniqueConstraints = @UniqueConstraint(columnNames = {"url", "action"})
)
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;
}
