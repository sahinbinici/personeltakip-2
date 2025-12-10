package com.bidb.personetakip.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock SMS Controller for development/test environments.
 * Logs SMS messages instead of actually sending them.
 * 
 * Enable with: mock.sms.enabled=true
 */
@RestController
@RequestMapping("/mock-sms")
@ConditionalOnProperty(name = "mock.sms.enabled", havingValue = "true")
public class MockSmsController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockSmsController.class);
    
    @GetMapping
    public ResponseEntity<String> sendSmsGet(@RequestParam Map<String, String> params) {
        String phones = params.get("phones");
        String message = params.get("message");
        String sender = params.get("sender");
        
        // Log the SMS content prominently
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║                    MOCK SMS SENT                               ║");
        logger.info("╠════════════════════════════════════════════════════════════════╣");
        logger.info("║ To:      {}                                              ║", phones);
        logger.info("║ From:    {}                                        ║", sender);
        logger.info("║ Message: {}                                                    ║", message);
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        
        // Also print to console for visibility
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📱 MOCK SMS SENT");
        System.out.println("=".repeat(70));
        System.out.println("To:      " + phones);
        System.out.println("From:    " + sender);
        System.out.println("Message: " + message);
        System.out.println("=".repeat(70) + "\n");
        
        // Return success response in VatanSMS format
        return ResponseEntity.ok("00");
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> sendSmsPost(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String message = request.get("message");
        String sender = request.get("sender");
        
        // Log the SMS content prominently
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║                    MOCK SMS SENT                               ║");
        logger.info("╠════════════════════════════════════════════════════════════════╣");
        logger.info("║ To:      {}                                              ║", to);
        logger.info("║ From:    {}                                        ║", sender);
        logger.info("║ Message: {}                                                    ║", message);
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        
        // Also print to console for visibility
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📱 MOCK SMS SENT");
        System.out.println("=".repeat(70));
        System.out.println("To:      " + to);
        System.out.println("From:    " + sender);
        System.out.println("Message: " + message);
        System.out.println("=".repeat(70) + "\n");
        
        // Return success response
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "SMS sent successfully (mock)",
            "to", to,
            "messageId", "mock-" + System.currentTimeMillis()
        ));
    }
}
