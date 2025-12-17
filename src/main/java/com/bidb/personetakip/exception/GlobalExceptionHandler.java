package com.bidb.personetakip.exception;

import com.bidb.personetakip.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Global exception handler for all REST API endpoints
 * Returns consistent error response format with timestamp, status, message, and path
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle ValidationException → HTTP 400
     * Thrown when input validation fails
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, 
            HttpServletRequest request) {
        logger.warn("Validation error: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle bean validation errors from @Valid annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("Validation failed");
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle validation errors raised as ConstraintViolationException.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getMessage())
            .orElse("Validation failed");
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle missing request parameters (e.g., required query params).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle OTP verification related errors → HTTP 400.
     */
    @ExceptionHandler({OtpVerificationException.class, OtpNotVerifiedException.class})
    public ResponseEntity<ErrorResponse> handleOtpErrors(
            RuntimeException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle personnel not found → HTTP 404.
     */
    @ExceptionHandler(PersonnelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePersonnelNotFound(
            PersonnelNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle AuthenticationException → HTTP 401
     * Thrown when authentication fails
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, 
            HttpServletRequest request) {
        logger.warn("Authentication error: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle ResourceNotFoundException → HTTP 404
     * Thrown when a requested resource is not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        logger.warn("Resource not found: {} at {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle ExternalServiceException → HTTP 503
     * Thrown when external service (SMS gateway, external database) fails
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, 
            HttpServletRequest request) {
        logger.error("External service error: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Handle IP validation errors → HTTP 400
     * Thrown when IP address format validation fails
     */
    @ExceptionHandler(IpValidationException.class)
    public ResponseEntity<ErrorResponse> handleIpValidationException(
            IpValidationException ex,
            HttpServletRequest request) {
        logger.warn("IP validation error: {} at {}", ex.getMessage(), request.getRequestURI());
        
        String detailedMessage = ex.getMessage();
        if (ex.getInvalidIpAddress() != null && ex.getValidationReason() != null) {
            detailedMessage = String.format("Invalid IP address '%s': %s", 
                ex.getInvalidIpAddress(), ex.getValidationReason());
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            detailedMessage,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle IP capture failures → HTTP 200 (non-blocking) or HTTP 500 (blocking)
     * Thrown when IP address capture fails during entry/exit operations
     */
    @ExceptionHandler(IpCaptureException.class)
    public ResponseEntity<ErrorResponse> handleIpCaptureException(
            IpCaptureException ex,
            HttpServletRequest request) {
        
        if (ex.shouldBlockOperation()) {
            // Blocking failure - return 500
            logger.error("Critical IP capture failure: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
            
            ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "IP capture failed and operation cannot continue: " + ex.getMessage(),
                request.getRequestURI()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } else {
            // Non-blocking failure - log warning and return 200 with warning message
            logger.warn("Non-critical IP capture failure: {} at {}", ex.getMessage(), request.getRequestURI());
            
            ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.OK.value(),
                "Warning",
                "Operation completed but IP capture failed: " + ex.getMessage(),
                request.getRequestURI()
            );
            
            return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
        }
    }
    
    /**
     * Handle IP assignment errors → HTTP 400
     * Thrown when IP assignment operations fail
     */
    @ExceptionHandler(IpAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleIpAssignmentException(
            IpAssignmentException ex,
            HttpServletRequest request) {
        logger.warn("IP assignment error: {} at {}", ex.getMessage(), request.getRequestURI());
        
        String detailedMessage = ex.getMessage();
        if (ex.getAssignmentOperation() != null && ex.getUserId() != null) {
            detailedMessage = String.format("IP assignment %s failed for user %s: %s", 
                ex.getAssignmentOperation(), ex.getUserId(), ex.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            detailedMessage,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle IP privacy configuration errors → HTTP 500
     * Thrown when IP privacy configuration operations fail
     */
    @ExceptionHandler(IpPrivacyConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleIpPrivacyConfigurationException(
            IpPrivacyConfigurationException ex,
            HttpServletRequest request) {
        logger.error("IP privacy configuration error: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        String detailedMessage = ex.getMessage();
        if (ex.getConfigurationKey() != null && ex.getConfigurationOperation() != null) {
            detailedMessage = String.format("IP privacy configuration %s failed for key '%s': %s", 
                ex.getConfigurationOperation(), ex.getConfigurationKey(), ex.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            detailedMessage,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle general Exception → HTTP 500
     * Catches all unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, 
            HttpServletRequest request) {
        logger.error("Unexpected error: {} at {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
