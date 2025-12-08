package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.EntryExitRecordRepository;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for GPS coordinate range validation.
 * 
 * Feature: personnel-tracking-system, Property 18: GPS coordinate range validation
 * Validates: Requirements 12.2, 12.3
 * 
 * For any GPS coordinates in an entry/exit request, latitude should be rejected if outside 
 * the range [-90, 90] and longitude should be rejected if outside the range [-180, 180].
 */
@RunWith(JUnitQuickcheck.class)
public class GpsCoordinateRangeValidationPropertyTest {
    
    @Mock
    private EntryExitRecordRepository entryExitRecordRepository;
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    @Mock
    private QrCodeService qrCodeService;
    
    private EntryExitService entryExitService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        entryExitService = new EntryExitServiceImpl(
            entryExitRecordRepository, 
            qrCodeRepository, 
            qrCodeService
        );
    }
    
    /**
     * Property: Valid GPS coordinates within range are accepted
     */
    @Property(trials = 100)
    public void validCoordinatesAccepted(@From(GpsCoordinateGenerator.class) GpsCoordinate coord,
                                         @From(UserIdGenerator.class) Long userId,
                                         @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Setup mocks
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.ENTRY));
        when(entryExitRecordRepository.save(any()))
            .thenAnswer(invocation -> {
                var record = invocation.getArgument(0);
                return record;
            });
        
        // Valid coordinates should not throw exception
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                coord.getLatitude(), coord.getLongitude()
            );
            // Success - no exception thrown
        } catch (ValidationException e) {
            fail("Valid coordinates should be accepted: lat=" + coord.getLatitude() + 
                 ", lon=" + coord.getLongitude() + ", error=" + e.getMessage());
        }
    }
    
    /**
     * Property: Latitude outside [-90, 90] is rejected
     */
    @Property(trials = 100)
    public void invalidLatitudeRejected(@From(UserIdGenerator.class) Long userId,
                                        @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDateTime timestamp = LocalDateTime.now();
        Double validLongitude = 0.0;
        
        // Test latitude > 90
        Double tooHighLatitude = 90.1;
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                tooHighLatitude, validLongitude
            );
            fail("Latitude > 90 should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention latitude", 
                e.getMessage().toLowerCase().contains("latitude"));
        }
        
        // Test latitude < -90
        Double tooLowLatitude = -90.1;
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                tooLowLatitude, validLongitude
            );
            fail("Latitude < -90 should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention latitude", 
                e.getMessage().toLowerCase().contains("latitude"));
        }
    }
    
    /**
     * Property: Longitude outside [-180, 180] is rejected
     */
    @Property(trials = 100)
    public void invalidLongitudeRejected(@From(UserIdGenerator.class) Long userId,
                                         @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDateTime timestamp = LocalDateTime.now();
        Double validLatitude = 0.0;
        
        // Test longitude > 180
        Double tooHighLongitude = 180.1;
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                validLatitude, tooHighLongitude
            );
            fail("Longitude > 180 should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention longitude", 
                e.getMessage().toLowerCase().contains("longitude"));
        }
        
        // Test longitude < -180
        Double tooLowLongitude = -180.1;
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                validLatitude, tooLowLongitude
            );
            fail("Longitude < -180 should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention longitude", 
                e.getMessage().toLowerCase().contains("longitude"));
        }
    }
    
    /**
     * Property: Boundary values are accepted
     */
    @Property(trials = 100)
    public void boundaryValuesAccepted(@From(UserIdGenerator.class) Long userId,
                                       @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDate today = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Setup mocks
        QrCode qrCode = createQrCode(userId, today, qrCodeValue, 0);
        when(qrCodeRepository.findByQrCodeValue(qrCodeValue))
            .thenReturn(Optional.of(qrCode));
        when(qrCodeService.validateQrCode(qrCodeValue, userId))
            .thenReturn(new QrCodeValidationDto(true, "Valid", EntryExitType.ENTRY));
        when(entryExitRecordRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Test all boundary combinations
        double[][] boundaries = {
            {90.0, 180.0},   // Max lat, max lon
            {90.0, -180.0},  // Max lat, min lon
            {-90.0, 180.0},  // Min lat, max lon
            {-90.0, -180.0}, // Min lat, min lon
            {0.0, 0.0}       // Origin
        };
        
        for (double[] coords : boundaries) {
            try {
                entryExitService.recordEntryExit(
                    userId, qrCodeValue, timestamp, 
                    coords[0], coords[1]
                );
                // Success - boundary values accepted
            } catch (ValidationException e) {
                fail("Boundary values should be accepted: lat=" + coords[0] + 
                     ", lon=" + coords[1] + ", error=" + e.getMessage());
            }
        }
    }
    
    /**
     * Property: Null coordinates are rejected
     */
    @Property(trials = 100)
    public void nullCoordinatesRejected(@From(UserIdGenerator.class) Long userId,
                                        @From(QrCodeValueGenerator.class) String qrCodeValue) {
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Test null latitude
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                null, 0.0
            );
            fail("Null latitude should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention coordinates", 
                e.getMessage().toLowerCase().contains("coordinate"));
        }
        
        // Test null longitude
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                0.0, null
            );
            fail("Null longitude should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention coordinates", 
                e.getMessage().toLowerCase().contains("coordinate"));
        }
        
        // Test both null
        try {
            entryExitService.recordEntryExit(
                userId, qrCodeValue, timestamp, 
                null, null
            );
            fail("Null coordinates should be rejected");
        } catch (ValidationException e) {
            assertTrue("Error message should mention coordinates", 
                e.getMessage().toLowerCase().contains("coordinate"));
        }
    }
    
    private QrCode createQrCode(Long userId, LocalDate date, String value, int usageCount) {
        QrCode qrCode = new QrCode();
        qrCode.setId(1L);
        qrCode.setUserId(userId);
        qrCode.setQrCodeValue(value);
        qrCode.setValidDate(date);
        qrCode.setUsageCount(usageCount);
        return qrCode;
    }
}
