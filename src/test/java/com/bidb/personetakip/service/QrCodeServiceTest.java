package com.bidb.personetakip.service;

import com.bidb.personetakip.dto.QrCodeDto;
import com.bidb.personetakip.dto.QrCodeValidationDto;
import com.bidb.personetakip.exception.ValidationException;
import com.bidb.personetakip.model.EntryExitType;
import com.bidb.personetakip.model.QrCode;
import com.bidb.personetakip.repository.QrCodeRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QrCodeService
 * Tests Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2
 */
@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    @InjectMocks
    private QrCodeServiceImpl qrCodeService;
    
    private Long testUserId;
    private LocalDate today;
    private QrCode testQrCode;
    
    @BeforeEach
    void setUp() {
        testUserId = 123L;
        today = LocalDate.now();
        
        testQrCode = QrCode.builder()
            .id(1L)
            .userId(testUserId)
            .qrCodeValue("TEST_QR_CODE_VALUE_123")
            .validDate(today)
            .usageCount(0)
            .build();
    }
    
    /**
     * Test daily QR code generation - Requirements 5.1, 5.2
     */
    @Test
    void getDailyQrCode_WhenNoExistingCode_ShouldGenerateNewCode() {
        // Arrange
        when(qrCodeRepository.findByUserIdAndValidDate(testUserId, today))
            .thenReturn(Optional.empty());
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> {
                QrCode qrCode = invocation.getArgument(0);
                qrCode.setId(1L);
                return qrCode;
            });
        
        // Act
        QrCodeDto result = qrCodeService.getDailyQrCode(testUserId);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.qrCodeValue());
        assertEquals(today, result.validDate());
        assertEquals(0, result.usageCount());
        assertEquals(2, result.maxUsage());
        
        verify(qrCodeRepository).findByUserIdAndValidDate(testUserId, today);
        verify(qrCodeRepository).save(argThat(qr -> 
            qr.getUserId().equals(testUserId) &&
            qr.getValidDate().equals(today) &&
            qr.getUsageCount() == 0
        ));
    }
    
    /**
     * Test QR code uniqueness for same user and date - Requirements 5.1, 5.2
     */
    @Test
    void getDailyQrCode_WhenExistingCode_ShouldReturnSameCode() {
        // Arrange
        when(qrCodeRepository.findByUserIdAndValidDate(testUserId, today))
            .thenReturn(Optional.of(testQrCode));
        
        // Act
        QrCodeDto result1 = qrCodeService.getDailyQrCode(testUserId);
        QrCodeDto result2 = qrCodeService.getDailyQrCode(testUserId);
        
        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.qrCodeValue(), result2.qrCodeValue());
        assertEquals(testQrCode.getQrCodeValue(), result1.qrCodeValue());
        
        verify(qrCodeRepository, times(2)).findByUserIdAndValidDate(testUserId, today);
        verify(qrCodeRepository, never()).save(any(QrCode.class));
    }
    
    /**
     * Test QR code initial state - Requirement 5.4
     */
    @Test
    void getDailyQrCode_NewCode_ShouldHaveInitialState() {
        // Arrange
        when(qrCodeRepository.findByUserIdAndValidDate(testUserId, today))
            .thenReturn(Optional.empty());
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        QrCodeDto result = qrCodeService.getDailyQrCode(testUserId);
        
        // Assert
        assertEquals(0, result.usageCount(), "Initial usage count should be 0");
        assertEquals(2, result.maxUsage(), "Maximum usage should be 2");
    }
    
    /**
     * Test QR code validation logic - Valid code - Requirements 5.3, 6.1
     */
    @Test
    void validateQrCode_WithValidCode_ShouldReturnValid() {
        // Arrange
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            testQrCode.getQrCodeValue(), 
            testUserId
        );
        
        // Assert
        assertTrue(result.valid());
        assertEquals("QR code is valid", result.message());
        assertEquals(EntryExitType.ENTRY, result.nextType());
        
        verify(qrCodeRepository).findByQrCodeValue(testQrCode.getQrCodeValue());
    }
    
    /**
     * Test QR code validation - Not found
     */
    @Test
    void validateQrCode_WhenNotFound_ShouldReturnInvalid() {
        // Arrange
        String nonExistentCode = "NON_EXISTENT_CODE";
        when(qrCodeRepository.findByQrCodeValue(nonExistentCode))
            .thenReturn(Optional.empty());
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(nonExistentCode, testUserId);
        
        // Assert
        assertFalse(result.valid());
        assertEquals("QR code not found", result.message());
        assertNull(result.nextType());
    }
    
    /**
     * Test QR code validation - Wrong owner - Requirement 8.2
     */
    @Test
    void validateQrCode_WhenWrongOwner_ShouldReturnInvalid() {
        // Arrange
        Long differentUserId = 999L;
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            testQrCode.getQrCodeValue(), 
            differentUserId
        );
        
        // Assert
        assertFalse(result.valid());
        assertEquals("QR code does not belong to user", result.message());
        assertNull(result.nextType());
    }
    
    /**
     * Test QR code validation - Invalid date - Requirements 5.3, 6.4
     */
    @Test
    void validateQrCode_WhenInvalidDate_ShouldReturnInvalid() {
        // Arrange
        QrCode oldQrCode = QrCode.builder()
            .id(1L)
            .userId(testUserId)
            .qrCodeValue("OLD_QR_CODE")
            .validDate(today.minusDays(1))
            .usageCount(0)
            .build();
        
        when(qrCodeRepository.findByQrCodeValue(oldQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(oldQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            oldQrCode.getQrCodeValue(), 
            testUserId
        );
        
        // Assert
        assertFalse(result.valid());
        assertEquals("QR code is not valid for today", result.message());
        assertNull(result.nextType());
    }
    
    /**
     * Test QR code validation - Usage limit exceeded - Requirements 6.1, 6.2
     */
    @Test
    void validateQrCode_WhenUsageLimitExceeded_ShouldReturnInvalid() {
        // Arrange
        QrCode usedQrCode = QrCode.builder()
            .id(1L)
            .userId(testUserId)
            .qrCodeValue("USED_QR_CODE")
            .validDate(today)
            .usageCount(2)
            .build();
        
        when(qrCodeRepository.findByQrCodeValue(usedQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(usedQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            usedQrCode.getQrCodeValue(), 
            testUserId
        );
        
        // Assert
        assertFalse(result.valid());
        assertEquals("QR code has already been used twice", result.message());
        assertNull(result.nextType());
    }
    
    /**
     * Test entry/exit type determination - Entry - Requirement 6.5
     */
    @Test
    void validateQrCode_WithZeroUsage_ShouldReturnEntryType() {
        // Arrange
        testQrCode.setUsageCount(0);
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            testQrCode.getQrCodeValue(), 
            testUserId
        );
        
        // Assert
        assertTrue(result.valid());
        assertEquals(EntryExitType.ENTRY, result.nextType());
    }
    
    /**
     * Test entry/exit type determination - Exit - Requirement 6.5
     */
    @Test
    void validateQrCode_WithOneUsage_ShouldReturnExitType() {
        // Arrange
        testQrCode.setUsageCount(1);
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        
        // Act
        QrCodeValidationDto result = qrCodeService.validateQrCode(
            testQrCode.getQrCodeValue(), 
            testUserId
        );
        
        // Assert
        assertTrue(result.valid());
        assertEquals(EntryExitType.EXIT, result.nextType());
    }
    
    /**
     * Test usage count increment - Requirements 6.1, 6.2
     */
    @Test
    void incrementUsageCount_WithValidCode_ShouldIncrementByOne() {
        // Arrange
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        qrCodeService.incrementUsageCount(testQrCode.getQrCodeValue());
        
        // Assert
        verify(qrCodeRepository).save(argThat(qr -> qr.getUsageCount() == 1));
    }
    
    /**
     * Test usage count increment - Multiple increments
     */
    @Test
    void incrementUsageCount_MultipleTimes_ShouldIncrementCorrectly() {
        // Arrange
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        when(qrCodeRepository.save(any(QrCode.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        qrCodeService.incrementUsageCount(testQrCode.getQrCodeValue());
        testQrCode.setUsageCount(1);
        qrCodeService.incrementUsageCount(testQrCode.getQrCodeValue());
        
        // Assert
        verify(qrCodeRepository, times(2)).save(any(QrCode.class));
        assertEquals(2, testQrCode.getUsageCount());
    }
    
    /**
     * Test usage count increment - Exceeds limit - Requirement 6.2
     */
    @Test
    void incrementUsageCount_WhenAtLimit_ShouldThrowException() {
        // Arrange
        testQrCode.setUsageCount(2);
        when(qrCodeRepository.findByQrCodeValue(testQrCode.getQrCodeValue()))
            .thenReturn(Optional.of(testQrCode));
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            qrCodeService.incrementUsageCount(testQrCode.getQrCodeValue());
        });
        
        assertEquals("QR code usage limit exceeded", exception.getMessage());
        verify(qrCodeRepository, never()).save(any(QrCode.class));
    }
    
    /**
     * Test usage count increment - Code not found
     */
    @Test
    void incrementUsageCount_WhenCodeNotFound_ShouldThrowException() {
        // Arrange
        String nonExistentCode = "NON_EXISTENT";
        when(qrCodeRepository.findByQrCodeValue(nonExistentCode))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            qrCodeService.incrementUsageCount(nonExistentCode);
        });
        
        assertEquals("QR code not found", exception.getMessage());
    }
    
    /**
     * Test QR code image generation - Requirement 5.5
     */
    @Test
    void generateQrCodeImage_WithValidValue_ShouldGenerateImage() {
        // Arrange
        String qrCodeValue = "TEST_QR_VALUE_FOR_IMAGE";
        
        // Act
        byte[] imageBytes = qrCodeService.generateQrCodeImage(qrCodeValue);
        
        // Assert
        assertNotNull(imageBytes);
        assertTrue(imageBytes.length > 0, "Image should have content");
    }
    
    /**
     * Test QR code image generation and decoding - Round trip - Requirement 5.5
     */
    @Test
    void generateQrCodeImage_ShouldBeDecodable() throws Exception {
        // Arrange
        String qrCodeValue = "ROUND_TRIP_TEST_VALUE";
        
        // Act
        byte[] imageBytes = qrCodeService.generateQrCodeImage(qrCodeValue);
        
        // Decode the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        BinaryBitmap binaryBitmap = new BinaryBitmap(
            new HybridBinarizer(new BufferedImageLuminanceSource(image))
        );
        Result result = new MultiFormatReader().decode(binaryBitmap);
        String decodedValue = result.getText();
        
        // Assert
        assertEquals(qrCodeValue, decodedValue, "Decoded value should match original");
    }
    
    /**
     * Test QR code image generation with different values
     */
    @Test
    void generateQrCodeImage_WithDifferentValues_ShouldGenerateDifferentImages() throws Exception {
        // Arrange
        String value1 = "QR_VALUE_1";
        String value2 = "QR_VALUE_2";
        
        // Act
        byte[] image1 = qrCodeService.generateQrCodeImage(value1);
        byte[] image2 = qrCodeService.generateQrCodeImage(value2);
        
        // Decode both images to verify they contain different values
        BufferedImage bufferedImage1 = ImageIO.read(new ByteArrayInputStream(image1));
        BinaryBitmap binaryBitmap1 = new BinaryBitmap(
            new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage1))
        );
        Result result1 = new MultiFormatReader().decode(binaryBitmap1);
        
        BufferedImage bufferedImage2 = ImageIO.read(new ByteArrayInputStream(image2));
        BinaryBitmap binaryBitmap2 = new BinaryBitmap(
            new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage2))
        );
        Result result2 = new MultiFormatReader().decode(binaryBitmap2);
        
        // Assert
        assertNotNull(image1);
        assertNotNull(image2);
        assertEquals(value1, result1.getText());
        assertEquals(value2, result2.getText());
        assertNotEquals(result1.getText(), result2.getText(), 
            "Different QR values should encode different data");
    }
}
