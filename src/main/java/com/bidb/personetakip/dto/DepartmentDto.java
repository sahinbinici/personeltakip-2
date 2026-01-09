package com.bidb.personetakip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for department information with user count.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    
    /**
     * Department code
     */
    private String code;
    
    /**
     * Department name
     */
    private String name;
    
    /**
     * Number of users in this department
     */
    private Long userCount;
}