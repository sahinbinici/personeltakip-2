package com.bidb.personetakip.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user information by admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    
    @Size(max = 20, message = "Sicil No en fazla 20 karakter olabilir")
    private String personnelNo;
    
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
    private String firstName;
    
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
    private String lastName;
    
    @Size(max = 15, message = "Telefon numarası en fazla 15 karakter olabilir")
    private String mobilePhone;
    
    @Size(max = 10, message = "Departman kodu en fazla 10 karakter olabilir")
    private String departmentCode;
    
    @Size(max = 200, message = "Departman adı en fazla 200 karakter olabilir")
    private String departmentName;
    
    @Size(max = 10, message = "Ünvan kodu en fazla 10 karakter olabilir")
    private String titleCode;
    
    @Size(min = 6, max = 50, message = "Şifre 6-50 karakter arasında olmalıdır")
    private String newPassword; // Optional - only update if provided
}
