package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an excuse report submitted by personnel.
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
@Entity
@Table(name = "excuses")
public class Excuse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @Column(name = "excuse_type_id", nullable = false)
    @NotNull(message = "Excuse type ID is required")
    private Long excuseTypeId;
    
    @Column(name = "excuse_type_name", nullable = false, length = 100)
    @NotBlank(message = "Excuse type name is required")
    @Size(max = 100, message = "Excuse type name cannot exceed 100 characters")
    private String excuseTypeName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Column(name = "excuse_date", nullable = false)
    @NotNull(message = "Excuse date is required")
    private LocalDate excuseDate;
    
    @Column(name = "submitted_at", nullable = false)
    @NotNull(message = "Submitted timestamp is required")
    private LocalDateTime submittedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExcuseStatus status;
    
    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // JSON array of attachment paths
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    
    // Constructors
    public Excuse() {
        this.submittedAt = LocalDateTime.now();
        this.status = ExcuseStatus.PENDING;
    }
    
    public Excuse(Long userId, Long excuseTypeId, String excuseTypeName, 
                  String description, LocalDate excuseDate) {
        this();
        this.userId = userId;
        this.excuseTypeId = excuseTypeId;
        this.excuseTypeName = excuseTypeName;
        this.description = description;
        this.excuseDate = excuseDate;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getExcuseTypeId() {
        return excuseTypeId;
    }
    
    public void setExcuseTypeId(Long excuseTypeId) {
        this.excuseTypeId = excuseTypeId;
    }
    
    public String getExcuseTypeName() {
        return excuseTypeName;
    }
    
    public void setExcuseTypeName(String excuseTypeName) {
        this.excuseTypeName = excuseTypeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getExcuseDate() {
        return excuseDate;
    }
    
    public void setExcuseDate(LocalDate excuseDate) {
        this.excuseDate = excuseDate;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public ExcuseStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExcuseStatus status) {
        this.status = status;
    }
    
    public String getAttachments() {
        return attachments;
    }
    
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public Long getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    // Business methods
    
    /**
     * Approves the excuse with admin notes.
     * 
     * @param adminId ID of the admin approving
     * @param notes Optional admin notes
     */
    public void approve(Long adminId, String notes) {
        this.status = ExcuseStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
    }
    
    /**
     * Rejects the excuse with admin notes.
     * 
     * @param adminId ID of the admin rejecting
     * @param notes Optional admin notes
     */
    public void reject(Long adminId, String notes) {
        this.status = ExcuseStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
    }
    
    /**
     * Checks if the excuse is still pending review.
     * 
     * @return true if pending
     */
    public boolean isPending() {
        return status == ExcuseStatus.PENDING;
    }
    
    /**
     * Checks if the excuse has been reviewed.
     * 
     * @return true if reviewed (approved or rejected)
     */
    public boolean isReviewed() {
        return status == ExcuseStatus.APPROVED || status == ExcuseStatus.REJECTED;
    }
    
    @Override
    public String toString() {
        return "Excuse{" +
                "id=" + id +
                ", userId=" + userId +
                ", excuseTypeId=" + excuseTypeId +
                ", excuseTypeName='" + excuseTypeName + '\'' +
                ", excuseDate=" + excuseDate +
                ", status=" + status +
                ", submittedAt=" + submittedAt +
                '}';
    }
}