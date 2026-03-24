package com.esign.entities.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContributorRequest {
    @NotNull(message = "Contributor ids is required")
    @Size(min = 1, message = "At least 1 contributor is required")
    private List<@NotBlank(message = "Contributor id must not be blank") String> contributorIds;
}
