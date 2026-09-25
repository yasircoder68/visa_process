package mca.visareg.registration.holiday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Service
public class WorkingDayService {

    private static final Logger log = LoggerFactory.getLogger(WorkingDayService.class);

    private final HolidayProvider holidayProvider;

    public WorkingDayService(HolidayProvider holidayProvider) {
        this.holidayProvider = holidayProvider;
    }

    public LocalDate nextWorkingDay(LocalDate from) {
        if (from == null) {
            throw new IllegalArgumentException("From date cannot be null");
        }

        LocalDate candidate = from.plusDays(1);
        while (isNonWorkingDay(candidate)) {
            candidate = candidate.plusDays(1);
        }

        log.info("Calculated next working day: from {} -> {}", from, candidate);
        return candidate;
    }

    public boolean isWorkingDay(LocalDate date) {
        return !isNonWorkingDay(date);
    }

    private boolean isNonWorkingDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return true;
        }

        Set<LocalDate> yearHolidays = holidayProvider.getHolidays(date.getYear());
        return yearHolidays != null && yearHolidays.contains(date);
    }
}
