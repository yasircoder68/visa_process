package mca.visareg.registration.holiday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
public class CompositeHolidayProvider implements HolidayProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeHolidayProvider.class);

    private final CalendarificHolidayProvider calendarificProvider;
    private final LocalFileHolidayProvider localFileProvider;
    private final Map<Integer, Set<LocalDate>> cache = new ConcurrentHashMap<>();

    public CompositeHolidayProvider(CalendarificHolidayProvider calendarificProvider,
                                    LocalFileHolidayProvider localFileProvider) {
        this.calendarificProvider = calendarificProvider;
        this.localFileProvider = localFileProvider;
    }

    @Override
    public Set<LocalDate> getHolidays(int year) {
        return cache.computeIfAbsent(year, y -> {
            log.info("Resolving holidays for year {}", y);
            Set<LocalDate> holidays = calendarificProvider.getHolidays(y);
            if (holidays == null || holidays.isEmpty()) {
                log.info("Using bundled local JSON holidays for year {}", y);
                holidays = localFileProvider.getHolidays(y);
            }
            return holidays;
        });
    }

    public void clearCache() {
        cache.clear();
    }
}
