package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single field-level check, as returned by the AI service (and echoed
 * back to the frontend). field names match the AI service's JSON exactly.
 */
@Data
@NoArgsConstructor
public class AiCheckDto {
    private String field;
    private String status;
    private String expected;
    private String actual;
    private String evidence;
}
