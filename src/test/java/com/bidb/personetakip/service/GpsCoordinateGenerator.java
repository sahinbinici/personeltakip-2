package com.bidb.personetakip.service;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for GPS coordinates (latitude and longitude pairs)
 */
public class GpsCoordinateGenerator extends Generator<GpsCoordinate> {
    
    public GpsCoordinateGenerator() {
        super(GpsCoordinate.class);
    }
    
    @Override
    public GpsCoordinate generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate valid latitude: -90 to 90
        double latitude = random.nextDouble(-90.0, 90.0);
        
        // Generate valid longitude: -180 to 180
        double longitude = random.nextDouble(-180.0, 180.0);
        
        return new GpsCoordinate(latitude, longitude);
    }
}

/**
 * Simple record to hold GPS coordinate pairs
 */
class GpsCoordinate {
    private final double latitude;
    private final double longitude;
    
    public GpsCoordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
}
