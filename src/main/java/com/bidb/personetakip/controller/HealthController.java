package com.bidb.personetakip.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    @Qualifier("dataSource")
    private final DataSource localDataSource;

    @Qualifier("externalDataSource")
    private final DataSource externalDataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());

        // Check local database connection
        boolean localDbHealthy = checkDatabaseConnection(localDataSource, "Local");
        response.put("localDatabase", localDbHealthy ? "UP" : "DOWN");

        // Check external database connection
        boolean externalDbHealthy = checkDatabaseConnection(externalDataSource, "External");
        response.put("externalDatabase", externalDbHealthy ? "UP" : "DOWN");

        boolean overallHealthy = localDbHealthy && externalDbHealthy;
        response.put("overallStatus", overallHealthy ? "HEALTHY" : "DEGRADED");

        return ResponseEntity.status(overallHealthy ? 200 : 503).body(response);
    }

    private boolean checkDatabaseConnection(DataSource dataSource, String dbName) {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5 second timeout
            if (isValid) {
                log.debug("{} database connection is healthy", dbName);
            } else {
                log.warn("{} database connection is not valid", dbName);
            }
            return isValid;
        } catch (SQLException e) {
            log.error("{} database connection failed: {}", dbName, e.getMessage());
            return false;
        }
    }
}