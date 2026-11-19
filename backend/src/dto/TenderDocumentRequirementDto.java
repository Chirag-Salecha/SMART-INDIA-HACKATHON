package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenderDocumentRequirementDto {
    @NotNull(message = "documentType is required")
    private DocumentType documentType;

    private boolean mandatory = true;
}
