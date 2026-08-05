package com.neueda.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for CSV validation results (before submission).
 */
public record CSVValidationResultDTO(
    /**
     * Total records in CSV.
     */
    Integer totalRecords,

    /**
     * Number of valid records ready for processing.
     */
    Integer validRecords,

    /**
     * Number of invalid records with errors.
     */
    Integer invalidRecords,

    /**
     * Whether all records are valid.
     */
    Boolean isValid,

    /**
     * List of validation errors by row.
     */
    List<CSVValidationErrorDTO> errors
) {}

