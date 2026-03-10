package com.esign.entities.profile;

import com.esign.annotation.profile.ValidImageFile;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadAvatarRequest {
    @NotNull(message = "Image is required")
    @ValidImageFile(
            maxSize = 2 * 1024 * 1024  // 2MB
    )
    private MultipartFile avatar;
}
