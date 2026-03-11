package com.esign.model;

import com.esign.constant.TableName;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = TableName.T_SIGNATURE)
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private DocumentContributor contributor;

    @Column(name = "signature_image", columnDefinition = "TEXT", nullable = false)
    private String signatureImage;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "position_x", nullable = false)
    private Float positionX;

    @Column(name = "position_y", nullable = false)
    private Float positionY;

    @Column(name = "certificate", columnDefinition = "TEXT")
    private String certificate;

    @Column(name = "signed_at", nullable = false)
    private LocalDateTime signedAt;

    @PrePersist
    protected void onCreate() {
        signedAt = LocalDateTime.now();
    }
}
