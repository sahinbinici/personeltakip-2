package com.bidb.personetakip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for manual user creation by admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualUserCreateDto {
    
    @NotBlank(message = "TC Kimlik No zorunludur")
    @Pattern(regexp = "\\d{11}", message = "TC Kimlik No 11 haneli olmalıdır")
    private String tcNo;
    
    @NotBlank(message = "Sicil No zorunludur")
    @Size(max = 20, message = "Sicil No en fazla 20 karakter olabilir")
    private String personnelNo;
    
    @NotBlank(message = "Ad zorunludur")
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir")
    private String firstName;
    
    @NotBlank(message = "Soyad zorunludur")
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir")
    private String lastName;
    
    @NotBlank(message = "Telefon numarası zorunludur")
    @Size(max = 15, message = "Telefon numarası en fazla 15 karakter olabilir")
    private String mobilePhone;
    
    @Size(max = 10, message = "Departman kodu en fazla 10 karakter olabilir")
    private String departmentCode;
    
    @Size(max = 200, message = "Departman adı en fazla 200 karakter olabilir")
    private String departmentName;
    
    @Size(max = 10, message = "Ünvan kodu en fazla 10 karakter olabilir")
    private String titleCode;
    
    private String role; // Default: NORMAL_USER
    
    @Size(min = 6, max = 50, message = "Şifre 6-50 karakter arasında olmalıdır")
    private String password;
}
