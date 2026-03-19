package com.esign.entities.document;

import com.esign.constant.ContributorStatus;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentContributorResponse {
    private String id;
    private String userId;
    private String username;
    private ContributorStatus status;
    private String signedAt;
    private String reason;
}
