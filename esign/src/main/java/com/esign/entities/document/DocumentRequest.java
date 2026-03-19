package com.esign.entities.document;

import com.esign.annotation.document.ValidDoc;
import com.esign.annotation.profile.ValidImageFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentRequest {
    @NotNull(message = "Image is required")
    @ValidDoc(
            maxSize = 2 * 1024 * 1024  // 2MB
    )
    private MultipartFile document;

    @NotBlank(message = "Title is required")
    private String title;

    private String folderId; // null = root

    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Deadline date must be in format yyyy-MM-dd"
    )
    private String deadline; // null = no deadline

    private List<String> contributorIds; // null = DRAFT, diisi = WAITING_SIGNATURE
}
