package com.neueda.dto;

/**
 * Request body for admin fraud decision (approve/reject)
 */
public class AdminFraudDecisionRequest {
    private String reviewedBy;
    private String notes;

    public AdminFraudDecisionRequest() {}

    public AdminFraudDecisionRequest(String reviewedBy, String notes) {
        this.reviewedBy = reviewedBy;
        this.notes = notes;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

