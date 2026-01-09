package com.bidb.personetakip.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for serving admin HTML pages.
 * All endpoints require ADMIN, DEPARTMENT_ADMIN, or SUPER_ADMIN role.
 * 
 * Requirements: 1.1, 1.3, 4.1, 4.5 - Admin role-based routing and access control
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEPARTMENT_ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminController {
    
    /**
     * Serves the admin dashboard page.
     * 
     * @return admin dashboard template name
     * Requirements: 1.1 - Admin dashboard access
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin-dashboard";
    }
    
    /**
     * Serves the user management page.
     * 
     * @return user management template name
     * Requirements: 2.1 - User management interface
     */
    @GetMapping("/users")
    public String users() {
        return "admin-users";
    }
    
    /**
     * Serves the entry/exit records management page.
     * 
     * @return records management template name
     * Requirements: 3.1 - Entry/exit records interface
     */
    @GetMapping("/records")
    public String records() {
        return "admin-records";
    }
    
    /**
     * Serves the department permissions management page.
     * Only ADMIN and SUPER_ADMIN can access this page.
     * 
     * @return department permissions template name
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @GetMapping("/department-permissions")
    public String departmentPermissions() {
        return "admin-department-permissions";
    }
}