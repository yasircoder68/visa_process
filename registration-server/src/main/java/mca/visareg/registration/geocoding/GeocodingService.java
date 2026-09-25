package mca.visareg.registration.geocoding;

import mca.visareg.registration.exception.RegistrationBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final RestClient restClient;
    private final String userAgent;
    private long lastCallTimestamp = 0;

    public GeocodingService(@Value("${nominatim.email:amdyasircoder@gmail.com}") String contactEmail) {
        this.userAgent = "visareg-student/1.0 (" + contactEmail + ")";
        this.restClient = RestClient.builder()
                .baseUrl(NOMINATIM_URL)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    public synchronized GeocodingResult resolve(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new RegistrationBusinessException("Address cannot be empty for geocoding");
        }

        // Rate guard: guarantee at least 1000ms between calls per Nominatim policy
        long now = System.currentTimeMillis();
        long diff = now - lastCallTimestamp;
        if (diff < 1000) {
            try {
                Thread.sleep(1000 - diff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTimestamp = System.currentTimeMillis();

        log.info("Geocoding address via Nominatim: '{}'", address);

        List<NominatimPlace> results;
        try {
            results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", address.trim())
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", "1")
                            .queryParam("limit", "1")
                            .queryParam("countrycodes", "in")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NominatimPlace>>() {});
        } catch (Exception e) {
            log.error("Failed to call Nominatim: {}", e.getMessage());
            throw new RegistrationBusinessException("Geocoding failed for address: " + address + " (" + e.getMessage() + ")");
        }

        if (results == null || results.isEmpty()) {
            log.warn("No geocoding results found for address: '{}'", address);
            throw new RegistrationBusinessException("Address not found: " + address);
        }

        NominatimPlace place = results.get(0);
        Double lat;
        Double lon;
        try {
            lat = Double.parseDouble(place.getLat());
            lon = Double.parseDouble(place.getLon());
        } catch (Exception e) {
            throw new RegistrationBusinessException("Invalid coordinates returned for address: " + address);
        }

        String district = extractDistrict(place.getAddress());
        if (district == null || district.isEmpty()) {
            district = "Chennai"; // fallback to default district if unspecific
        }

        log.info("Resolved address '{}' -> lat={}, lon={}, district='{}'", address, lat, lon, district);
        return new GeocodingResult(lat, lon, district);
    }

    public String extractDistrict(Map<String, String> address) {
        if (address == null) return null;

        String raw = null;
        if (address.get("state_district") != null) {
            raw = address.get("state_district");
        } else if (address.get("county") != null) {
            raw = address.get("county");
        } else if (address.get("city") != null) {
            raw = address.get("city");
        } else if (address.get("town") != null) {
            raw = address.get("town");
        }

        if (raw == null) {
            return null;
        }

        // Normalise: trim and strip " District"
        return raw.replaceAll("(?i)\\s+District$", "").trim();
    }
}
