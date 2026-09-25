package mca.visareg.registration;

import mca.visareg.registration.geocoding.GeocodingService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeocodingServiceTests {

    private final GeocodingService geocodingService = new GeocodingService("test@example.com");

    @Test
    void testDistrictExtractionAndNormalization() {
        Map<String, String> addr1 = new HashMap<>();
        addr1.put("state_district", "Chennai District");
        assertEquals("Chennai", geocodingService.extractDistrict(addr1));

        Map<String, String> addr2 = new HashMap<>();
        addr2.put("county", "Coimbatore District");
        assertEquals("Coimbatore", geocodingService.extractDistrict(addr2));

        Map<String, String> addr3 = new HashMap<>();
        addr3.put("city", "Madurai");
        assertEquals("Madurai", geocodingService.extractDistrict(addr3));

        Map<String, String> addr4 = new HashMap<>();
        addr4.put("town", "Tambaram");
        assertEquals("Tambaram", geocodingService.extractDistrict(addr4));
    }
}
