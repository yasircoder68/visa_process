package mca.visareg.registration.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
public class LocalFileHolidayProvider implements HolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalFileHolidayProvider.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        String filename = "holidays/holidays-IN-" + year + ".json";
        ClassPathResource resource = new ClassPathResource(filename);

        if (!resource.exists()) {
            log.warn("Bundled holiday file not found: '{}'", filename);
            return holidays;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode holidayArray = root.path("holidays");
            if (holidayArray.isArray()) {
                for (JsonNode item : holidayArray) {
                    String dateStr = item.path("date").asText();
                    if (dateStr != null && !dateStr.isEmpty()) {
                        holidays.add(LocalDate.parse(dateStr));
                    }
                }
            }
            log.info("Loaded {} gazetted holidays from bundled file '{}'", holidays.size(), filename);
        } catch (Exception e) {
            log.error("Failed to parse bundled holiday file '{}': {}", filename, e.getMessage());
        }

        return holidays;
    }
}
