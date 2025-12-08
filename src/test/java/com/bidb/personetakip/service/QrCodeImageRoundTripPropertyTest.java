package com.bidb.personetakip.service;

import com.bidb.personetakip.repository.QrCodeRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Property-based test for QR code image round-trip.
 * 
 * Feature: personnel-tracking-system, Property 13: QR code image round-trip
 * Validates: Requirements 5.5
 * 
 * For any QR Code Value, generating a QR code image and then decoding it 
 * should produce the original QR Code Value.
 */
@RunWith(JUnitQuickcheck.class)
public class QrCodeImageRoundTripPropertyTest {
    
    @Mock
    private QrCodeRepository qrCodeRepository;
    
    private QrCodeService qrCodeService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        qrCodeService = new QrCodeServiceImpl(qrCodeRepository);
    }
    
    /**
     * Property: Encoding a QR code value to image and decoding it back produces the original value
     */
    @Property(trials = 100)
    public void qrCodeImageRoundTrip(@From(QrCodeValueGenerator.class) String qrCodeValue) {
        try {
            // Generate QR code image from the value
            byte[] imageBytes = qrCodeService.generateQrCodeImage(qrCodeValue);
            
            assertNotNull("Generated image should not be null", imageBytes);
            assertTrue("Generated image should have content", imageBytes.length > 0);
            
            // Decode the image back to text
            String decodedValue = decodeQrCodeImage(imageBytes);
            
            // Verify round-trip: decoded value should match original
            assertEquals("Decoded QR code value should match original value",
                qrCodeValue, decodedValue);
            
        } catch (Exception e) {
            fail("QR code round-trip should not throw exception: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to decode QR code image bytes back to text
     */
    private String decodeQrCodeImage(byte[] imageBytes) throws IOException, NotFoundException {
        // Convert byte array to BufferedImage
        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        
        assertNotNull("BufferedImage should not be null", bufferedImage);
        
        // Decode QR code from image
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        
        MultiFormatReader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap);
        
        return result.getText();
    }
}
