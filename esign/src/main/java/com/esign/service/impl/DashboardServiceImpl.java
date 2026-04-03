package com.esign.service.impl;

import com.esign.constant.ContributorStatus;
import com.esign.constant.DocumentStatus;
import com.esign.entities.dashboard.*;
import com.esign.model.Document;
import com.esign.model.DocumentActivity;
import com.esign.model.DocumentContributor;
import com.esign.model.User;
import com.esign.repository.DocumentActivityRepository;
import com.esign.repository.DocumentContributorRepository;
import com.esign.repository.DocumentRepository;
import com.esign.service.AuthService;
import com.esign.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final AuthService authService;
    private final DocumentRepository documentRepository;
    private final DocumentContributorRepository documentContributorRepository;
    private final DocumentActivityRepository documentActivityRepository;

    @Transactional(readOnly = true)
    @Override
    public DashboardResponse getDashboard() {
        User user = authService.getAuthenticatedUser();
        LocalDateTime now = LocalDateTime.now();

        List<Document> myDocuments = documentRepository.findAllByOwnerAndIsDeletedFalse(user);

        List<DocumentContributor> myContributions = documentContributorRepository.findAllByUserAndDocument_IsDeletedFalse(user);

        DashboardSummary summary = buildSummary(myDocuments, myContributions, now);

        // document urgent, butuh ttd (2hari kedepan)
        LocalDateTime urgentTime = now.plusDays(2);
        List<UrgentDocumentResponse> urgent = buildUrgent(myDocuments, myContributions, now, urgentTime);

        // my document active (in progres & waiting)
        List<ActiveDocumentResponse> myActiveDocuments = myDocuments.stream()
                .filter(d -> d.getStatus() == DocumentStatus.IN_PROGRESS
                        || d.getStatus() == DocumentStatus.WAITING_SIGNATURE)
                .map(d -> toActiveDocumentResponse(d, now))
                .toList();

        // recent activities document
        LocalDateTime threeDaysAgo = now.minusDays(3);
        List<RecentActivityResponse> recentActivity = documentActivityRepository
                .findAllByDocumentInAndCreatedAtAfterOrderByCreatedAtDesc(myDocuments, threeDaysAgo)
                .stream()
                .map(this::toRecentActivityResponse)
                .toList();

        return DashboardResponse.builder()
                .summary(summary)
                .urgent(urgent)
                .myActiveDocuments(myActiveDocuments)
                .recentActivity(recentActivity)
                .build();
    }

    private DashboardSummary buildSummary(
            List<Document> myDocuments,
            List<DocumentContributor> myContributions,
            LocalDateTime now
    ) {
        return DashboardSummary.builder()
                .totalMyDocuments((int) myDocuments.size())
                .draft((int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.DRAFT).count())
                .waitingSignature((int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.WAITING_SIGNATURE).count())
                .inProgress((int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.IN_PROGRESS).count())
                .completed((int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.COMPLETED).count())
                .rejected((int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.REJECTED).count())
                .needMySignature((int) myContributions.stream().filter(c -> c.getStatus() == ContributorStatus.PENDING).count())
                .overdue((int) myDocuments.stream()
                        .filter(d -> d.getDeadline() != null
                                && d.getDeadline().isBefore(now)
                                && d.getStatus() != DocumentStatus.COMPLETED)
                        .count())
                .build();
    }

    private List<UrgentDocumentResponse> buildUrgent(
            List<Document> myDocuments,
            List<DocumentContributor> myContributions,
            LocalDateTime now,
            LocalDateTime threshold
    ) {
        List<UrgentDocumentResponse> result = new ArrayList<>();

        // dokumen orang lain yg harus current user tanda tangani
        myContributions.stream()
                .filter(c -> c.getStatus() == ContributorStatus.PENDING
                        && c.getDocument().getDeadline() != null
                        && c.getDocument().getDeadline().isBefore(threshold))
                .forEach(c -> {
                    Document doc = c.getDocument();
                    result.add(UrgentDocumentResponse.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .fileName(doc.getFileName())
                            .documentStatus(doc.getStatus().name())
                            .urgentType("NEED_MY_SIGNATURE")
                            .ownerName(doc.getOwner().getUsername())
                            .pendingContributors(null)
                            .deadline(doc.getDeadline().toString())
                            .isOverdue(doc.getDeadline().isBefore(now))
                            .build());
                });

        // dokumen milik current user yg menunggu orang lain tanda tangan
        myDocuments.stream()
                .filter(d -> (d.getStatus() == DocumentStatus.IN_PROGRESS
                        || d.getStatus() == DocumentStatus.WAITING_SIGNATURE)
                        && d.getDeadline() != null
                        && d.getDeadline().isBefore(threshold))
                .forEach(d -> {
                    long pending = d.getContributors().stream()
                            .filter(c -> c.getStatus() == ContributorStatus.PENDING)
                            .count();
                    result.add(UrgentDocumentResponse.builder()
                            .id(d.getId())
                            .title(d.getTitle())
                            .fileName(d.getFileName())
                            .documentStatus(d.getStatus().name())
                            .urgentType("WAITING_OTHERS")
                            .ownerName(d.getOwner().getUsername())
                            .pendingContributors((int) pending)
                            .deadline(d.getDeadline().toString())
                            .isOverdue(d.getDeadline().isBefore(now))
                            .build());
                });

        // Urutkan, overdue dulu, lalu deadline terdekat
        result.sort(Comparator
                .comparing(UrgentDocumentResponse::getIsOverdue).reversed()
                .thenComparing(UrgentDocumentResponse::getDeadline));

        return result;
    }

    private ActiveDocumentResponse toActiveDocumentResponse(Document doc, LocalDateTime now) {
        long signed = doc.getContributors().stream()
                .filter(c -> c.getStatus() == ContributorStatus.SIGNED).count();
        long pending = doc.getContributors().stream()
                .filter(c -> c.getStatus() == ContributorStatus.PENDING).count();

        return ActiveDocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus().name())
                .folderName(doc.getFolder() != null ? doc.getFolder().getName() : null)
                .totalContributors(doc.getContributors().size())
                .signedContributors((int) signed)
                .pendingContributors((int) pending)
                .deadline(doc.getDeadline() != null ? doc.getDeadline().toString() : null)
                .isOverdue(doc.getDeadline() != null && doc.getDeadline().isBefore(now))
                .updatedAt(doc.getUpdatedAt().toString())
                .build();
    }

    private RecentActivityResponse toRecentActivityResponse(DocumentActivity activity) {
        return RecentActivityResponse.builder()
                .documentId(activity.getDocument().getId())
                .documentTitle(activity.getDocument().getTitle())
                .activityType(activity.getActivityType().name())
                .description(activity.getDescription())
                .triggeredBy(activity.getTriggeredBy().getUsername())
                .createdAt(activity.getCreatedAt().toString())
                .build();
    }

}
