package mca.visareg.registration.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class CalendarificHolidayProvider implements HolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(CalendarificHolidayProvider.class);
    private static final String CALENDARIFIC_URL = "https://calendarific.com/api/v2/holidays";

    private final String apiKey;
    private final RestClient restClient;

    public CalendarificHolidayProvider(@Value("${holiday.api.key:${HOLIDAY_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.restClient = RestClient.builder().baseUrl(CALENDARIFIC_URL).build();
    }

    @Override
    public Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        if (apiKey.isEmpty()) {
            log.info("No HOLIDAY_API_KEY provided; skipping Calendarific API call.");
            return holidays;
        }

        try {
            log.info("Fetching Indian holidays from Calendarific for year {}...", year);
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("api_key", apiKey)
                            .queryParam("country", "IN")
                            .queryParam("year", year)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root != null) {
                JsonNode holidayArray = root.path("response").path("holidays");
                if (holidayArray.isArray()) {
                    for (JsonNode item : holidayArray) {
                        String isoDate = item.path("date").path("iso").asText();
                        if (isoDate != null && isoDate.length() >= 10) {
                            holidays.add(LocalDate.parse(isoDate.substring(0, 10)));
                        }
                    }
                }
            }
            log.info("Fetched {} holidays from Calendarific for year {}", holidays.size(), year);
        } catch (Exception e) {
            log.warn("Calendarific API call failed: {}. Falling back to local file.", e.getMessage());
        }

        return holidays;
    }
}
