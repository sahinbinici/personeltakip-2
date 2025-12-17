package com.bidb.personetakip.service;

import com.bidb.personetakip.exception.IpAssignmentException;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of IP compliance service for mismatch detection and reporting.
 * Requirements: 3.4, 4.1, 4.2, 4.3, 4.4
 */
@Service
public class IpComplianceServiceImpl implements IpComplianceService {
    
    private static final Logger logger = LoggerFactory.getLogger(IpComplianceServiceImpl.class);
    
    @Autowired
    IpAddressService ipAddressService; // Package-private for testing
    
    @Override
    public boolean isIpAddressMatch(String actualIp, String assignedIpAddresses) {
        if (actualIp == null || assignedIpAddresses == null || assignedIpAddresses.trim().isEmpty()) {
            return false;
        }
        
        // If actual IP is unknown, it's not a match
        if (ipAddressService.getUnknownIpDefault().equals(actualIp)) {
            return false;
        }
        
        List<String> assignedIps = parseAssignedIpAddresses(assignedIpAddresses);
        String formattedActualIp = ipAddressService.formatIpAddress(actualIp);
        
        return assignedIps.stream()
                .map(ipAddressService::formatIpAddress)
                .anyMatch(formattedActualIp::equals);
    }
    
    @Override
    public List<String> parseAssignedIpAddresses(String assignedIpAddresses) {
        if (assignedIpAddresses == null || assignedIpAddresses.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Split by comma or semicolon, trim whitespace, and filter out empty strings
        return Arrays.stream(assignedIpAddresses.split("[,;]"))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasIpMismatch(EntryExitRecord record, User user) {
        if (record == null || user == null) {
            return false;
        }
        
        // No mismatch if user has no assigned IP addresses
        if (user.getAssignedIpAddresses() == null || user.getAssignedIpAddresses().trim().isEmpty()) {
            return false;
        }
        
        // No mismatch if record IP is unknown (we can't compare)
        if (record.getIpAddress() == null || 
            ipAddressService.getUnknownIpDefault().equals(record.getIpAddress())) {
            return false;
        }
        
        // Mismatch if IP doesn't match any assigned IP
        return !isIpAddressMatch(record.getIpAddress(), user.getAssignedIpAddresses());
    }
    
    @Override
    public IpComplianceStatus getIpComplianceStatus(EntryExitRecord record, User user) {
        if (record == null || user == null) {
            return IpComplianceStatus.UNKNOWN_IP;
        }
        
        // Check if user has assigned IP addresses
        if (user.getAssignedIpAddresses() == null || user.getAssignedIpAddresses().trim().isEmpty()) {
            return IpComplianceStatus.NO_ASSIGNMENT;
        }
        
        // Check if record IP is unknown
        if (record.getIpAddress() == null || 
            ipAddressService.getUnknownIpDefault().equals(record.getIpAddress())) {
            return IpComplianceStatus.UNKNOWN_IP;
        }
        
        // Check for match or mismatch
        if (isIpAddressMatch(record.getIpAddress(), user.getAssignedIpAddresses())) {
            return IpComplianceStatus.MATCH;
        } else {
            return IpComplianceStatus.MISMATCH;
        }
    }
    
    @Override
    public boolean validateAssignedIpAddresses(String assignedIpAddresses) {
        if (assignedIpAddresses == null || assignedIpAddresses.trim().isEmpty()) {
            return true; // Empty assignment is valid
        }
        
        List<String> ipAddresses = parseAssignedIpAddresses(assignedIpAddresses);
        
        // All parsed IP addresses must be valid
        return ipAddresses.stream()
                .allMatch(ipAddressService::isValidIpAddress);
    }
    
    /**
     * Validates assigned IP addresses with detailed error handling
     * Requirements: 3.1, 3.2
     */
    public void validateAssignedIpAddressesWithException(String assignedIpAddresses, String userId) throws IpAssignmentException {
        if (assignedIpAddresses == null) {
            // Null is valid (no assignment)
            return;
        }
        
        if (assignedIpAddresses.trim().isEmpty()) {
            // Empty is valid (no assignment)
            return;
        }
        
        try {
            List<String> ipAddresses = parseAssignedIpAddresses(assignedIpAddresses);
            
            if (ipAddresses.isEmpty()) {
                throw new IpAssignmentException("No valid IP addresses found in assignment", "validate", userId, assignedIpAddresses);
            }
            
            // Check each IP address individually
            for (String ipAddress : ipAddresses) {
                if (!ipAddressService.isValidIpAddress(ipAddress)) {
                    throw new IpAssignmentException(
                        String.format("Invalid IP address format: %s", ipAddress), 
                        "validate", userId, ipAddress);
                }
            }
            
            // Check for duplicates
            long uniqueCount = ipAddresses.stream().distinct().count();
            if (uniqueCount != ipAddresses.size()) {
                throw new IpAssignmentException("Duplicate IP addresses found in assignment", "validate", userId, assignedIpAddresses);
            }
            
            // Check maximum number of assignments (configurable limit)
            int maxAssignments = 10; // Could be made configurable
            if (ipAddresses.size() > maxAssignments) {
                throw new IpAssignmentException(
                    String.format("Too many IP addresses assigned (max: %d, found: %d)", maxAssignments, ipAddresses.size()), 
                    "validate", userId, assignedIpAddresses);
            }
            
        } catch (IpAssignmentException e) {
            // Re-throw IP assignment exceptions
            throw e;
        } catch (Exception e) {
            throw new IpAssignmentException("Unexpected error during IP assignment validation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Safely removes IP assignment with error handling
     * Requirements: 3.5
     */
    public void validateIpAssignmentRemoval(String currentAssignment, String ipToRemove, String userId) throws IpAssignmentException {
        if (currentAssignment == null || currentAssignment.trim().isEmpty()) {
            throw new IpAssignmentException("Cannot remove IP from empty assignment", "remove", userId, ipToRemove);
        }
        
        if (ipToRemove == null || ipToRemove.trim().isEmpty()) {
            throw new IpAssignmentException("IP address to remove cannot be empty", "remove", userId, ipToRemove);
        }
        
        try {
            List<String> currentIps = parseAssignedIpAddresses(currentAssignment);
            String formattedIpToRemove = ipAddressService.formatIpAddress(ipToRemove);
            
            boolean found = currentIps.stream()
                    .map(ipAddressService::formatIpAddress)
                    .anyMatch(formattedIpToRemove::equals);
            
            if (!found) {
                throw new IpAssignmentException(
                    String.format("IP address %s not found in current assignment", ipToRemove), 
                    "remove", userId, ipToRemove);
            }
            
        } catch (IpAssignmentException e) {
            // Re-throw IP assignment exceptions
            throw e;
        } catch (Exception e) {
            throw new IpAssignmentException("Unexpected error during IP assignment removal validation: " + e.getMessage(), e);
        }
    }
}