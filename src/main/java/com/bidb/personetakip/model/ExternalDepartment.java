package com.bidb.personetakip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing department/unit data from external read-only database.
 * This entity is mapped to the external database 'brkodu' table.
 * 
 * External DB: 193.140.136.45
 * Database: isicil
 * Table: brkodu
 */
@Entity
@Table(name = "brkodu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalDepartment {
    
    /**
     * Department/Unit code - Primary Key
     */
    @Id
    @Column(name = "BRKODU", length = 20)
    private String brkodu;
    
    /**
     * Department/Unit description (Birim Açıklama)
     */
    @Column(name = "BRKDAC", length = 255)
    private String brkdac;
}
