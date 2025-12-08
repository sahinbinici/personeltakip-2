package com.bidb.personetakip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing telephone data from external read-only database.
 * This entity is mapped to the external database 'telefo' table.
 */
@Entity
@Table(name = "telefo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalTelephone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Employee/Personnel number (Sicil No)
     */
    @Column(name = "esicno")
    private Long esicno;
    
    /**
     * Telephone number
     */
    @Column(name = "telefo", length = 15)
    private String telefo;
    
    /**
     * Get mobile phone (alias for telefo)
     */
    public String getMobilePhone() {
        return this.telefo;
    }
    
    /**
     * Set mobile phone (alias for telefo)
     */
    public void setMobilePhone(String mobilePhone) {
        this.telefo = mobilePhone;
    }
}
