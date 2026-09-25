package mca.visareg.registration;

import mca.visareg.registration.holiday.LocalFileHolidayProvider;
import mca.visareg.registration.holiday.WorkingDayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkingDayServiceTests {

    private WorkingDayService workingDayService;

    @BeforeEach
    void setUp() {
        LocalFileHolidayProvider holidayProvider = new LocalFileHolidayProvider();
        workingDayService = new WorkingDayService(holidayProvider);
    }

    @Test
    void testNextWorkingDaySkipsWeekendAndMondayHoliday() {
        // In 2026, January 26 (Republic Day) is a Monday.
        // The Friday before is January 23, 2026.
        // Expected:
        // - Jan 24 (Sat) -> skipped
        // - Jan 25 (Sun) -> skipped
        // - Jan 26 (Mon, Republic Day) -> skipped
        // - Jan 27 (Tue) -> returned!
        LocalDate fridayBefore = LocalDate.of(2026, 1, 23);
        LocalDate nextWorkingDay = workingDayService.nextWorkingDay(fridayBefore);

        assertEquals(LocalDate.of(2026, 1, 27), nextWorkingDay,
                "Given a known holiday on a Monday (Republic Day 2026-01-26), nextWorkingDay(the Friday before) must return Tuesday.");
    }

    @Test
    void testNextWorkingDayNormalWeekend() {
        // Friday Jan 9, 2026 -> Saturday 10 & Sunday 11 skipped -> Monday Jan 12, 2026
        LocalDate friday = LocalDate.of(2026, 1, 9);
        LocalDate nextWorkingDay = workingDayService.nextWorkingDay(friday);

        assertEquals(LocalDate.of(2026, 1, 12), nextWorkingDay);
    }

    @Test
    void testNextWorkingDayAcrossYearBoundary() {
        // Thursday Dec 31, 2026 -> Friday Jan 1, 2027
        LocalDate dec31 = LocalDate.of(2026, 12, 31);
        LocalDate nextWorkingDay = workingDayService.nextWorkingDay(dec31);

        assertEquals(LocalDate.of(2027, 1, 1), nextWorkingDay);
    }
}
