package mca.visareg.registration.holiday;

import java.time.LocalDate;
import java.util.Set;

public interface HolidayProvider {
    Set<LocalDate> getHolidays(int year);
}
