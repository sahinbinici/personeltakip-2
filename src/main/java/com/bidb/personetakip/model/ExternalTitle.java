package com.bidb.personetakip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing title/position data from external read-only database.
 * This entity is mapped to the external database 'unvkod' table.
 * 
 * External DB: 193.140.136.45
 * Database: isicil
 * Table: unvkod
 */
@Entity
@Table(name = "unvkod")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalTitle {
    
    /**
     * Title/Position code - Primary Key
     */
    @Id
    @Column(name = "unvkod", length = 20)
    private String unvkod;
    
    /**
     * Title/Position description (Ünvan Açıklama)
     */
    @Column(name = "unvack", length = 255)
    private String unvack;
}
