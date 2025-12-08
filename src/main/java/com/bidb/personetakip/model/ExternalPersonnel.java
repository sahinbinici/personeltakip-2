package com.bidb.personetakip.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing personnel master data from external read-only database.
 * This entity is mapped to the external database 'person' table and should only be used for reading.
 * 
 * External DB: 193.140.136.45
 * Database: isicil
 * Table: person (with joins to unvkod, brkodu, telefo)
 */
@Entity
@Table(name = "person")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalPersonnel {
    
    /**
     * Employee/Personnel number (Sicil No) - Primary Key
     */
    @Id
    @Column(name = "esicno")
    private Long esicno;
    
    /**
     * Turkish Citizen ID Number (TC Kimlik No) - 11 digits
     */
    @Column(name = "tckiml", length = 11)
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    private String tckiml;
    
    /**
     * First name (Ad)
     */
    @Column(name = "peradi", length = 100)
    @NotBlank(message = "First name is required")
    private String peradi;
    
    /**
     * Last name (Soyad)
     */
    @Column(name = "soyadi", length = 100)
    @NotBlank(message = "Last name is required")
    private String soyadi;
    
    /**
     * Unit code (Birim Kodu)
     */
    @Column(name = "brkodu", length = 20)
    private String brkodu;
    
    /**
     * Title code (Ünvan Kodu)
     */
    @Column(name = "unvkod", length = 20)
    private String unvkod;
    
    // Helper methods to match the expected interface
    
    /**
     * Get TC number (alias for tckiml)
     */
    public String getTcNo() {
        return this.tckiml;
    }
    
    /**
     * Set TC number (alias for tckiml)
     */
    public void setTcNo(String tcNo) {
        this.tckiml = tcNo;
    }
    
    /**
     * Get personnel number (alias for esicno)
     */
    public String getPersonnelNo() {
        return this.esicno != null ? this.esicno.toString() : null;
    }
    
    /**
     * Set personnel number (alias for esicno)
     */
    public void setPersonnelNo(String personnelNo) {
        this.esicno = personnelNo != null ? Long.parseLong(personnelNo) : null;
    }
    
    /**
     * Get first name (alias for peradi)
     */
    public String getFirstName() {
        return this.peradi;
    }
    
    /**
     * Set first name (alias for peradi)
     */
    public void setFirstName(String firstName) {
        this.peradi = firstName;
    }
    
    /**
     * Get last name (alias for soyadi)
     */
    public String getLastName() {
        return this.soyadi;
    }
    
    /**
     * Set last name (alias for soyadi)
     */
    public void setLastName(String lastName) {
        this.soyadi = lastName;
    }
    
    /**
     * Get user ID (alias for esicno)
     */
    public Long getUserId() {
        return this.esicno;
    }
    
    /**
     * Set user ID (alias for esicno)
     */
    public void setUserId(Long userId) {
        this.esicno = userId;
    }
    
    /**
     * Get mobile phone - Note: This should be fetched from telefo table separately
     * This is a placeholder method for compatibility
     */
    public String getMobilePhone() {
        return null; // Will be fetched from ExternalTelephone table
    }
    
    /**
     * Set mobile phone - Note: This is a placeholder for compatibility
     */
    public void setMobilePhone(String mobilePhone) {
        // No-op: phone is in separate table
    }
}
