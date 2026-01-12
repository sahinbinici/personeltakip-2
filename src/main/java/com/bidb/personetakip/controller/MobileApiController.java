package com.bidb.personetakip.controller;

import com.bidb.personetakip.dto.AuthTokenDto;
import com.bidb.personetakip.dto.EntryExitRecordDto;
import com.bidb.personetakip.dto.EntryExitRequestDto;
import com.bidb.personetakip.dto.ExcuseRequestDto;
import com.bidb.personetakip.dto.ExcuseResponseDto;
import com.bidb.personetakip.dto.ExcuseTypeDto;
import com.bidb.personetakip.dto.LoginRequest;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.dto.SimpleExcuseRequestDto;
import com.bidb.personetakip.dto.UserStatusDto;
import com.bidb.personetakip.model.EntryExitRecord;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.User;
import com.bidb.personetakip.model.UserRole;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.UserRepository;
import com.bidb.personetakip.service.AuthenticationService;
import com.bidb.personetakip.service.EntryExitService;
import com.bidb.personetakip.service.ExcuseService;
import com.bidb.personetakip.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller for mobile application API endpoints.
 * Requirements: 7.1, 7.2, 8.1, 8.2, 8.5
 */
@RestController
@RequestMapping("/api/mobil")
@Tag(name = "Mobile API", description = "Mobile application endpoints for authentication and entry/exit recording")
public class MobileApiController {
    
    private final AuthenticationService authenticationService;
    private final EntryExitService entryExitService;
    private final QrCodeService qrCodeService;
    private final ExcuseService excuseService;
    private final EntryExitRecordRepository entryExitRecordRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Simple in-memory rate limiting (20 requests per minute per user)
    private final Map<Long, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    
    @Value("${qr.code.development-mode:false}")
    private boolean developmentMode;
    
    public MobileApiController(
            AuthenticationService authenticationService,
            EntryExitService entryExitService,
            QrCodeService qrCodeService,
            ExcuseService excuseService,
            EntryExitRecordRepository entryExitRecordRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationService = authenticationService;
        this.entryExitService = entryExitService;
        this.qrCodeService = qrCodeService;
        this.excuseService = excuseService;
        this.entryExitRecordRepository = entryExitRecordRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * Mobile authentication endpoint.
     * POST /api/mobil/login
     * 
     * @param request Login request with TC No and password
     * @return AuthTokenDto containing JWT token
     * Requirements: 7.1 - Validate credentials against local database
     *               7.2 - Generate JWT token with personnel ID and role
     */
    @Operation(
        summary = "Mobile user authentication",
        description = "Authenticates mobile users with TC No and password, returns JWT token for subsequent API calls"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthTokenDto.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "userId": 123,
                        "role": "USER",
                        "expiresAt": "2024-12-17T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    value = """
                    {
                        "message": "Invalid TC No or password"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Rate Limited",
                    value = """
                    {
                        "message": "Too many login attempts. Please try again later."
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthTokenDto> login(
        @Parameter(description = "Login credentials with TC No and password", required = true)
        @Valid @RequestBody LoginRequest request) {
        AuthTokenDto authToken = authenticationService.login(
            request.tcNo(), 
            request.password()
        );
        return ResponseEntity.ok(authToken);
    }
    
    /**
     * Records entry/exit event with QR code data.
     * POST /api/mobil/giris-cikis-kaydet
     * 
     * @param request Entry/exit request with QR code, timestamp, and GPS coordinates
     * @param httpRequest HTTP servlet request for IP address extraction
     * @return EntryExitRecordDto containing recorded event data
     * Requirements: 8.1 - Extract JWT token from request headers
     *               8.2 - Validate QR code against current date and personnel ID
     *               8.5 - Store entry/exit record with all required fields
     *               1.1 - Capture client IP address from HTTP request
     *               1.2 - Handle proxy and load balancer scenarios
     *               6.5 - Support IP tracking configuration control
     */
    @Operation(
        summary = "Record entry/exit event",
        description = "Records personnel entry or exit event using QR code validation with GPS coordinates and timestamp",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Entry/exit recorded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EntryExitRecordDto.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "id": 456,
                        "userId": 123,
                        "timestamp": "2024-12-16T08:30:00Z",
                        "latitude": 41.0082,
                        "longitude": 28.9784,
                        "ipAddress": "192.168.1.100",
                        "recordType": "ENTRY"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Invalid QR Code",
                        value = """
                        {
                            "message": "Invalid or expired QR code"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Invalid GPS",
                        value = """
                        {
                            "message": "Invalid GPS coordinates"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Rate Limited",
                    value = """
                    {
                        "message": "Too many requests. Please try again later."
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/giris-cikis-kaydet")
    public ResponseEntity<?> recordEntryExit(
            @Parameter(description = "Entry/exit request with QR code, timestamp, and GPS coordinates", required = true)
            @Valid @RequestBody EntryExitRequestDto request,
            HttpServletRequest httpRequest) {
        Long userId = getAuthenticatedUserId();
        
        // Check rate limit
        if (!checkRateLimit(userId)) {
            return ResponseEntity.status(429)
                .body(Map.of("message", "Too many requests. Please try again later."));
        }
        
        // Validate QR code
        QrCodeValidationDto validation = qrCodeService.validateQrCode(
            request.qrCodeValue(), 
            userId
        );
        
        if (!validation.valid()) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", validation.message()));
        }
        
        // Validate GPS coordinates
        if (!isValidGpsCoordinate(request.latitude(), request.longitude())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid GPS coordinates"));
        }
        
        // Record entry/exit with IP address capture
        // Requirements: 1.1, 1.2, 6.2, 6.5 - IP tracking with graceful failure handling
        EntryExitRecordDto record = entryExitService.recordEntryExit(
            userId,
            request.qrCodeValue(),
            request.timestamp(),
            request.latitude(),
            request.longitude(),
            httpRequest  // Pass HTTP request for IP address extraction
        );
        
        return ResponseEntity.ok(record);
    }
    
    /**
     * Submits an excuse request.
     * POST /api/mobil/mazeret-bildir
     * 
     * @param request Excuse request with type, description, date, and attachments
     * @return ExcuseResponseDto containing submission result
     * Requirements: 3.1 - Validate excuse type selection
     *               3.2 - Validate description requirements based on excuse type
     *               3.3 - Validate minimum 10 character description
     *               3.4 - Submit excuse to backend API
     *               3.5 - Display success confirmation
     */
    @Operation(
        summary = "Submit excuse request",
        description = "Submits an excuse request for a specific date with type, description, and optional attachments",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Excuse submitted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExcuseResponseDto.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "id": 789,
                        "message": "Mazeret bildiriminiz başarıyla gönderildi. Onay sürecinde size bilgi verilecektir.",
                        "status": "PENDING",
                        "submittedAt": "2024-12-16T10:30:00",
                        "userId": 123
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid excuse request",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Missing Description",
                        value = """
                        {
                            "message": "Bu mazeret türü için açıklama gereklidir"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Short Description",
                        value = """
                        {
                            "message": "Açıklama en az 10 karakter olmalıdır"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Future Date",
                        value = """
                        {
                            "message": "Mazeret tarihi gelecek bir tarih olamaz"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate excuse for date",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Duplicate Excuse",
                    value = """
                    {
                        "message": "Bu tarih için zaten bir mazeret bildirimi bulunmaktadır"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/mazeret-bildir")
    public ResponseEntity<ExcuseResponseDto> submitExcuse(
            @Parameter(description = "Excuse request with type, description, date, and attachments", required = true)
            @Valid @RequestBody ExcuseRequestDto request) {
        Long userId = getAuthenticatedUserId();
        
        // Check rate limit
        if (!checkRateLimit(userId)) {
            return ResponseEntity.status(429)
                .body(ExcuseResponseDto.error("Çok fazla istek gönderildi. Lütfen daha sonra tekrar deneyin."));
        }
        
        ExcuseResponseDto response = excuseService.submitExcuse(userId, request);
        
        if (response.id() != null) {
            return ResponseEntity.ok(response);
        } else {
            // Determine appropriate HTTP status based on error message
            if (response.message().contains("zaten bir mazeret")) {
                return ResponseEntity.status(409).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        }
    }
    
    /**
     * Gets available excuse types.
     * GET /api/mobil/mazeret-turleri
     * 
     * @return List of available excuse types
     * Requirements: 3.1 - Provide excuse type selection options
     *               3.2 - Include description and attachment requirements
     */
    @Operation(
        summary = "Get excuse types",
        description = "Retrieves list of available excuse types with their requirements",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Excuse types retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExcuseTypeDto.class),
                examples = @ExampleObject(
                    name = "Excuse Types",
                    value = """
                    [
                        {
                            "id": 1,
                            "name": "Hastalık",
                            "requiresDescription": true,
                            "requiresAttachment": false
                        },
                        {
                            "id": 2,
                            "name": "Aile Acil Durumu",
                            "requiresDescription": true,
                            "requiresAttachment": false
                        },
                        {
                            "id": 3,
                            "name": "Resmi İzin",
                            "requiresDescription": false,
                            "requiresAttachment": true
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/mazeret-turleri")
    public ResponseEntity<java.util.List<ExcuseTypeDto>> getExcuseTypes() {
        java.util.List<ExcuseTypeDto> excuseTypes = excuseService.getExcuseTypes();
        return ResponseEntity.ok(excuseTypes);
    }
    
    /**
     * Gets current user status (inside/outside).
     * GET /api/mobil/durum
     * 
     * @return Current user status information
     */
    @Operation(
        summary = "Get current user status",
        description = "Retrieves the current entry/exit status for the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserStatusDto.class),
                examples = {
                    @ExampleObject(
                        name = "User Inside",
                        value = """
                        {
                            "isInside": true,
                            "lastActionType": "ENTRY",
                            "lastActionTime": "2024-12-16T08:30:00",
                            "message": "İçerisiniz"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "User Outside",
                        value = """
                        {
                            "isInside": false,
                            "lastActionType": "EXIT",
                            "lastActionTime": "2024-12-16T17:30:00",
                            "message": "Dışarıdasınız"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "No Records",
                        value = """
                        {
                            "isInside": false,
                            "lastActionType": null,
                            "lastActionTime": null,
                            "message": "Henüz giriş/çıkış kaydı yok"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                        "error": "Unauthorized",
                        "message": "Authentication required"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/durum")
    public ResponseEntity<UserStatusDto> getCurrentUserStatus() {
        Long userId = getAuthenticatedUserId();
        UserStatusDto status = entryExitService.getCurrentUserStatus(userId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * Records entry/exit with excuse (no QR code required).
     * POST /api/mobil/mazeret-kaydet
     * 
     * @param request Simple excuse request with excuse text
     * @param httpRequest HTTP servlet request for IP address extraction
     * @return EntryExitRecordDto containing recorded event data
     */
    @Operation(
        summary = "Record entry/exit with excuse",
        description = "Records personnel entry or exit event with an excuse text instead of QR code. GPS coordinates are not required.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Entry/exit with excuse recorded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EntryExitRecordDto.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                        "id": 456,
                        "userId": 123,
                        "timestamp": "2024-12-16T08:30:00Z",
                        "latitude": null,
                        "longitude": null,
                        "type": "ENTRY",
                        "excuse": "Doktor randevusu nedeniyle geç kaldım"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Excuse",
                    value = """
                    {
                        "message": "Mazeret metni 10-500 karakter arasında olmalıdır"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded"
        )
    })
    @PostMapping("/mazeret-kaydet")
    public ResponseEntity<?> recordEntryExitWithExcuse(
            @Parameter(description = "Simple excuse request with excuse text", required = true)
            @Valid @RequestBody SimpleExcuseRequestDto request,
            HttpServletRequest httpRequest) {
        Long userId = getAuthenticatedUserId();
        
        // Check rate limit
        if (!checkRateLimit(userId)) {
            return ResponseEntity.status(429)
                .body(Map.of("message", "Too many requests. Please try again later."));
        }
        
        // Validate excuse text
        if (request.excuse() == null || request.excuse().trim().length() < 10) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Mazeret metni en az 10 karakter olmalıdır"));
        }
        
        if (request.excuse().length() > 500) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Mazeret metni en fazla 500 karakter olabilir"));
        }
        
        // Determine entry/exit type
        EntryExitType type = null;
        if (request.type() != null && !request.type().isEmpty()) {
            try {
                type = EntryExitType.valueOf(request.type().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid type, will be determined automatically
            }
        }
        
        // Record entry/exit with excuse
        EntryExitRecordDto record = entryExitService.recordEntryExitWithExcuse(
            userId,
            request.excuse().trim(),
            type,
            LocalDateTime.now(),
            httpRequest
        );
        
        return ResponseEntity.ok(record);
    }
    
    /**
     * Development endpoint to reset QR code usage count.
     * POST /api/mobil/dev-reset-qr
     */
    @PostMapping("/dev-reset-qr")
    public ResponseEntity<Map<String, Object>> resetQrUsage() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        Long userId = getAuthenticatedUserId();
        
        // Reset QR code usage through service
        try {
            qrCodeService.resetUsageCount(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "QR code usage count reset successfully for user " + userId);
            result.put("developmentMode", developmentMode);
            result.put("userId", userId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Development endpoint to reset all entry/exit records for a user.
     * POST /api/mobil/dev-reset-records
     */
    @PostMapping("/dev-reset-records")
    public ResponseEntity<Map<String, Object>> resetEntryExitRecords() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        Long userId = getAuthenticatedUserId();
        
        try {
            // Delete all entry/exit records for the user
            entryExitService.resetUserRecords(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "All entry/exit records reset successfully for user " + userId);
            result.put("developmentMode", developmentMode);
            result.put("userId", userId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Development endpoint to reset both QR usage and entry/exit records.
     * POST /api/mobil/dev-reset-all
     */
    @PostMapping("/dev-reset-all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        Long userId = getAuthenticatedUserId();
        
        try {
            // Reset QR code usage
            qrCodeService.resetUsageCount(userId);
            
            // Reset all entry/exit records
            entryExitService.resetUserRecords(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "QR usage and entry/exit records reset successfully for user " + userId);
            result.put("developmentMode", developmentMode);
            result.put("userId", userId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Development endpoint to debug user records.
     * GET /api/mobil/dev-debug-records
     */
    @GetMapping("/dev-debug-records")
    public ResponseEntity<Map<String, Object>> debugUserRecords() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        Long userId = getAuthenticatedUserId();
        
        try {
            // Get user status
            UserStatusDto status = entryExitService.getCurrentUserStatus(userId);
            
            // Get recent records
            List<EntryExitRecord> recentRecords = entryExitRecordRepository.findLatestByUserId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("currentStatus", status);
            result.put("recentRecordsCount", recentRecords.size());
            
            if (!recentRecords.isEmpty()) {
                EntryExitRecord latest = recentRecords.get(0);
                result.put("latestRecord", Map.of(
                    "id", latest.getId(),
                    "type", latest.getType(),
                    "timestamp", latest.getTimestamp(),
                    "qrCodeValue", latest.getQrCodeValue()
                ));
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Development endpoint to create a test user.
     * POST /api/mobil/dev-create-test-user
     */
    @PostMapping("/dev-create-test-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        try {
            // Check if test user already exists
            String testTcNo = "12345678901";
            if (userRepository.existsByTcNo(testTcNo)) {
                return ResponseEntity.ok(Map.of(
                    "message", "Test user already exists",
                    "tcNo", testTcNo,
                    "password", "test123",
                    "role", "NORMAL_USER"
                ));
            }
            
            // Create test user
            User testUser = User.builder()
                .tcNo(testTcNo)
                .personnelNo("TEST001")
                .firstName("Test")
                .lastName("Kullanıcı")
                .mobilePhone("05551234567")
                .departmentCode("BIL")
                .departmentName("Bilgisayar Mühendisliği")
                .titleCode("ARS")
                .passwordHash(passwordEncoder.encode("test123"))
                .role(UserRole.NORMAL_USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            User savedUser = userRepository.save(testUser);
            
            // Create QR code for the test user
            try {
                qrCodeService.getDailyQrCode(savedUser.getId());
            } catch (Exception e) {
                // QR code creation failed, but user is created
                System.out.println("Warning: Could not create QR code for test user: " + e.getMessage());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Test user created successfully");
            result.put("userId", savedUser.getId());
            result.put("tcNo", testTcNo);
            result.put("password", "test123");
            result.put("role", "NORMAL_USER");
            result.put("personnelNo", "TEST001");
            result.put("name", "Test Kullanıcı");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Development endpoint to create a test department admin user.
     * POST /api/mobil/dev-create-department-admin
     */
    @PostMapping("/dev-create-department-admin")
    public ResponseEntity<Map<String, Object>> createDepartmentAdminUser() {
        if (!developmentMode) {
            return ResponseEntity.status(403).body(Map.of("error", "Only available in development mode"));
        }
        
        try {
            // Check if department admin already exists
            String adminTcNo = "11111111111";
            if (userRepository.existsByTcNo(adminTcNo)) {
                return ResponseEntity.ok(Map.of(
                    "message", "Department admin user already exists",
                    "tcNo", adminTcNo,
                    "password", "admin123",
                    "role", "DEPARTMENT_ADMIN",
                    "department", "Bilgisayar Mühendisliği"
                ));
            }
            
            // Create department admin user for BIL department (same as test user)
            User departmentAdmin = User.builder()
                .tcNo(adminTcNo)
                .personnelNo("ADMIN001")
                .firstName("Departman")
                .lastName("Yöneticisi")
                .mobilePhone("05559876543")
                .departmentCode("BIL")
                .departmentName("Bilgisayar Mühendisliği")
                .titleCode("DOC")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(UserRole.DEPARTMENT_ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            User savedAdmin = userRepository.save(departmentAdmin);
            
            // Create QR code for the department admin
            try {
                qrCodeService.getDailyQrCode(savedAdmin.getId());
            } catch (Exception e) {
                // QR code creation failed, but user is created
                System.out.println("Warning: Could not create QR code for department admin: " + e.getMessage());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Department admin user created successfully");
            result.put("userId", savedAdmin.getId());
            result.put("tcNo", adminTcNo);
            result.put("password", "admin123");
            result.put("role", "DEPARTMENT_ADMIN");
            result.put("personnelNo", "ADMIN001");
            result.put("name", "Departman Yöneticisi");
            result.put("department", "Bilgisayar Mühendisliği (BIL)");
            result.put("permissions", "Can manage users only in BIL department");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/dev-config")
    public ResponseEntity<Map<String, Object>> getDevConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("developmentMode", developmentMode);
        config.put("maxUsagePerDay", "Check QrCodeService for this value");
        config.put("rateLimitingDisabled", developmentMode);
        return ResponseEntity.ok(config);
    }
    
    /**
     * Validates GPS coordinates are within valid ranges.
     * 
     * @param latitude Latitude value
     * @param longitude Longitude value
     * @return true if coordinates are valid
     * Requirements: 12.2 - Validate latitude range [-90, 90]
     *               12.3 - Validate longitude range [-180, 180]
     */
    private boolean isValidGpsCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= -90.0 && latitude <= 90.0 
            && longitude >= -180.0 && longitude <= 180.0;
    }
    
    /**
     * Checks if the user has exceeded the rate limit.
     * In development mode, rate limiting is disabled.
     * 
     * @param userId User ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkRateLimit(Long userId) {
        // Skip rate limiting in development mode
        if (developmentMode) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis();
        
        rateLimitMap.compute(userId, (id, info) -> {
            if (info == null || currentTime - info.windowStart > RATE_LIMIT_WINDOW_MS) {
                // New window
                return new RateLimitInfo(currentTime, 1);
            } else {
                // Within current window
                info.count.incrementAndGet();
                return info;
            }
        });
        
        RateLimitInfo info = rateLimitMap.get(userId);
        return info.count.get() <= MAX_REQUESTS_PER_MINUTE;
    }
    
    /**
     * Extracts authenticated user ID from security context.
     * 
     * @return User ID from JWT token
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        // If principal is stored as a different type, extract accordingly
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return Long.parseLong((String) principal);
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
    
    /**
     * Inner class to track rate limit information per user.
     */
    private static class RateLimitInfo {
        final long windowStart;
        final AtomicInteger count;
        
        RateLimitInfo(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
