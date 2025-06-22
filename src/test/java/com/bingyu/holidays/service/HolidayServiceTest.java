package com.bingyu.holidays.service;

import com.bingyu.holidays.client.HolidayApiClient;
import com.bingyu.holidays.config.CountryCodeLoader;
import com.bingyu.holidays.dto.CountryHolidayCount;
import com.bingyu.holidays.dto.HolidayMapper;
import com.bingyu.holidays.dto.LastHolidayDTO;
import com.bingyu.holidays.exception.InvalidCountryCodeException;
import com.bingyu.holidays.model.Holiday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HolidayServiceTest {

    @Mock
    private HolidayApiClient apiClient;
    @Mock
    private CountryCodeLoader codeLoader;
    @Mock
    private HolidayMapper mapper;
    @InjectMocks
    private HolidayService service;

    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        service = new HolidayService(apiClient, mapper, directExecutor, codeLoader);
        when(mapper.toLastHolidayDTO(any(Holiday.class)))
                .thenAnswer(inv -> {
                    Holiday h = inv.getArgument(0);
                    return new LastHolidayDTO(h.getDate(), h.getName());
                });
    }

    @Test
    void getLastThreeHolidays_validCountry_returnsThreeMostRecent() {
        when(codeLoader.isValid("US")).thenReturn(true);
        LocalDate today = LocalDate.of(2025, 6, 22);
        List<Holiday> list = List.of(
                new Holiday(today.minusDays(1), "H1", "H1"),
                new Holiday(today.minusDays(2), "H2", "H2"),
                new Holiday(today.minusDays(3), "H3", "H3"),
                new Holiday(today.minusDays(4), "H4", "H4")
        );
        when(apiClient.fetchHolidays(today.getYear(), "US")).thenReturn(list);

        List<LastHolidayDTO> result = service.getLastThreeHolidays("US");
        assertEquals(3, result.size());
        assertEquals("H1", result.get(0).getName());
    }

    @Test
    void getLastThreeHolidays_invalidCountry_throwsException() {
        when(codeLoader.isValid("ZZ")).thenReturn(false);
        assertThrows(InvalidCountryCodeException.class,
                () -> service.getLastThreeHolidays("ZZ"));
    }

    @Test
    void countWeekdayHolidays_filtersWeekends() {
        when(codeLoader.isValid("US")).thenReturn(true);
        LocalDate monday = LocalDate.of(2025, 6, 23);
        LocalDate sunday = LocalDate.of(2025, 6, 22);
        List<Holiday> list = List.of(
                new Holiday(monday, "Mon", "Mon"),
                new Holiday(sunday, "Sun", "Sun")
        );
        when(apiClient.fetchHolidays(2025, "US")).thenReturn(list);

        List<CountryHolidayCount> counts = service.countWeekdayHolidays(2025, List.of("US"));
        assertEquals(1, counts.size());
        assertEquals(1, counts.get(0).getWeekdayHolidayCount());
    }

    @Test
    void findCommonHolidays_returnsIntersection() {
        when(codeLoader.isValid("A")).thenReturn(true);
        when(codeLoader.isValid("B")).thenReturn(true);
        LocalDate d = LocalDate.of(2025, 1, 1);
        Holiday hA = new Holiday(d, "NameA", "LocalA");
        Holiday hB = new Holiday(d, "NameB", "LocalB");
        when(apiClient.fetchHolidays(2025, "A")).thenReturn(List.of(hA));
        when(apiClient.fetchHolidays(2025, "B")).thenReturn(List.of(hB));

        List<LastHolidayDTO> common = service.findCommonHolidays(2025, "A", "B");
        assertEquals(1, common.size());
        assertEquals(d, common.get(0).getDate());
    }

    @Test
    void getLastThreeHolidays_whenThisYearLessThanThree_backfillsFromLastYear() {
        when(codeLoader.isValid("US")).thenReturn(true);
        LocalDate today = LocalDate.now();
        int thisYear = today.getYear();
        int lastYear = thisYear - 1;

        Holiday h1 = new Holiday(today.minusDays(5), "ThisYear1", "ThisYear1");
        when(apiClient.fetchHolidays(thisYear, "US")).thenReturn(List.of(h1));

        Holiday h2 = new Holiday(LocalDate.of(lastYear, 12, 31), "LastYear1", "LastYear1");
        Holiday h3 = new Holiday(LocalDate.of(lastYear, 11, 30), "LastYear2", "LastYear2");
        when(apiClient.fetchHolidays(lastYear, "US")).thenReturn(List.of(h2, h3));

        List<LastHolidayDTO> dtos = service.getLastThreeHolidays("US");
        assertEquals(3, dtos.size());
        assertEquals(h1.getDate(), dtos.get(0).getDate());
        assertEquals(h2.getDate(), dtos.get(1).getDate());
        assertEquals(h3.getDate(), dtos.get(2).getDate());
    }

    @Test
    void getLastThreeHolidays_whenThisYearHasNone_backfillsAllFromLastYear() {
        when(codeLoader.isValid("US")).thenReturn(true);
        LocalDate today = LocalDate.now();
        int thisYear = today.getYear();
        int lastYear = thisYear - 1;

        when(apiClient.fetchHolidays(thisYear, "US")).thenReturn(List.of());
        Holiday l1 = new Holiday(LocalDate.of(lastYear, 12, 25), "LY1", "LY1");
        Holiday l2 = new Holiday(LocalDate.of(lastYear, 11, 25), "LY2", "LY2");
        Holiday l3 = new Holiday(LocalDate.of(lastYear, 10, 25), "LY3", "LY3");
        when(apiClient.fetchHolidays(lastYear, "US")).thenReturn(List.of(l1, l2, l3));

        List<LastHolidayDTO> dtos = service.getLastThreeHolidays("US");
        assertEquals(3, dtos.size());
        assertEquals(l1.getDate(), dtos.get(0).getDate());
        assertEquals(l2.getDate(), dtos.get(1).getDate());
        assertEquals(l3.getDate(), dtos.get(2).getDate());
    }

}
