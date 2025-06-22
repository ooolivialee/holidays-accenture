package com.bingyu.holidays.service;

import com.bingyu.holidays.client.HolidayApiClient;
import com.bingyu.holidays.config.CountryCodeLoader;
import com.bingyu.holidays.dto.CountryHolidayCount;
import com.bingyu.holidays.dto.HolidayMapper;
import com.bingyu.holidays.dto.LastHolidayDTO;
import com.bingyu.holidays.exception.InvalidCountryCodeException;
import com.bingyu.holidays.model.Holiday;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {
    private static final int LAST_HOLIDAYS_LIMIT = 3;

    private final HolidayApiClient apiClient;
    private final HolidayMapper mapper;
    private final Executor executor;
    private final CountryCodeLoader codeLoader;

    public List<LastHolidayDTO> getLastThreeHolidays(String countryCode) {
        validateCountryCode(countryCode);
        return fetchHolidaysUpToToday(countryCode).stream()
                .sorted(Comparator.comparing(Holiday::getDate).reversed())
                .limit(LAST_HOLIDAYS_LIMIT)
                .map(mapper::toLastHolidayDTO)
                .collect(Collectors.toList());
    }

    public List<CountryHolidayCount> countWeekdayHolidays(int year, List<String> countries) {
        validateCountryCodes(countries);
        List<CompletableFuture<CountryHolidayCount>> futures = countries.stream()
                .map(code -> CompletableFuture.supplyAsync(() -> {
                    long count = apiClient.fetchHolidays(year, code).stream()
                            .map(Holiday::getDate)
                            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                                    && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                            .count();
                    return new CountryHolidayCount(code, (int) count);
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparingInt(CountryHolidayCount::getWeekdayHolidayCount).reversed())
                .collect(Collectors.toList());
    }

    public List<LastHolidayDTO> findCommonHolidays(int year, String countryA, String countryB) {
        validateCountryCode(countryA);
        validateCountryCode(countryB);
        List<Holiday> listA = apiClient.fetchHolidays(year, countryA);
        List<Holiday> listB = apiClient.fetchHolidays(year, countryB);

        Map<LocalDate, String> mapA = listA.stream()
                .collect(Collectors.toMap(Holiday::getDate, Holiday::getLocalName, (name1, name2) -> name1));

        return listB.stream()
                .filter(h -> mapA.containsKey(h.getDate()))
                .map(h -> new LastHolidayDTO(h.getDate(), mapA.get(h.getDate())))
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateCountryCode(String code) {
        if (!codeLoader.isValid(code)) {
            log.warn("Invalid country code attempted: {}", code);
            throw new InvalidCountryCodeException("Invalid country code: " + code);
        }
    }

    private void validateCountryCodes(List<String> codes) {
        codes.forEach(this::validateCountryCode);
    }

    /**
     * Fetches holidays up to today. If fewer than 3 have occurred this year,
     * also fetches last year's holidays.
     */
    private List<Holiday> fetchHolidaysUpToToday(String countryCode) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        List<Holiday> all = new ArrayList<>(apiClient.fetchHolidays(year, countryCode));
        long passed = all.stream()
                .filter(h -> !h.getDate().isAfter(today))
                .count();
        if (passed < LAST_HOLIDAYS_LIMIT) {
            all.addAll(apiClient.fetchHolidays(year - 1, countryCode));
        }
        return all.stream()
                .filter(h -> !h.getDate().isAfter(today))
                .collect(Collectors.toList());
    }
}
