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
 * This entity is mapped to the external database and should only be used for reading.
 */
@Entity
@Table(name = "personnel_master", indexes = {
    @Index(name = "idx_tc_personnel", columnList = "tc_no, personnel_no")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalPersonnel {
    
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * Turkish Citizen ID Number (TC Kimlik No) - 11 digits
     */
    @Column(name = "tc_no", unique = true, nullable = false, length = 11)
    @NotBlank(message = "TC number is required")
    @Pattern(regexp = "\\d{11}", message = "TC number must be exactly 11 digits")
    private String tcNo;
    
    /**
     * Personnel/Employee Number (Sicil No)
     */
    @Column(name = "personnel_no", nullable = false, length = 20)
    @NotBlank(message = "Personnel number is required")
    private String personnelNo;
    
    /**
     * First name
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    private String firstName;
    
    /**
     * Last name
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    /**
     * Mobile phone number
     */
    @Column(name = "mobile_phone", nullable = false, length = 15)
    @NotBlank(message = "Mobile phone is required")
    private String mobilePhone;
}
