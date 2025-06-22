package com.bingyu.holidays.controller;

import com.bingyu.holidays.dto.CountryHolidayCount;
import com.bingyu.holidays.dto.LastHolidayDTO;
import com.bingyu.holidays.exception.InvalidCountryCodeException;
import com.bingyu.holidays.service.HolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HolidayController.class)
public class HolidayControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HolidayService service;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public HolidayService service(){
            return mock(HolidayService.class);
        }
    }

    @Test
    @DisplayName("GET /holidays/last-three?country=US return three LastHolidayDTO")
    void lastThree_shouldReturnList() throws Exception {
        List<LastHolidayDTO> mockList = List.of(
                new LastHolidayDTO(LocalDate.of(2025, 6, 18), "Queen's Birthday"),
                new LastHolidayDTO(LocalDate.of(2025, 6, 10), "Eid al-Adha"),
                new LastHolidayDTO(LocalDate.of(2025, 5,  1), "Labor Day")
        );
        given(service.getLastThreeHolidays("US")).willReturn(mockList);

        mvc.perform(get("/holidays/last-three")
                        .param("country", "US")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(3)))
                .andExpect(jsonPath("$[0].date", is("2025-06-18")))
                .andExpect(jsonPath("$[0].name", is("Queen's Birthday")));
    }

    @Test
    @DisplayName("GET /holidays/weekday-count?year=2025&countries=US,DE return sorted CountryHolidayCount list")
    void weekdayCount_shouldReturnSortedCounts() throws Exception {
        List<CountryHolidayCount> mockCounts = List.of(
                new CountryHolidayCount("DE", 12),
                new CountryHolidayCount("US", 10)
        );
        given(service.countWeekdayHolidays(eq(2025), anyList())).willReturn(mockCounts);

        mvc.perform(get("/holidays/weekday-count")
                        .param("year", "2025")
                        .param("countries", "US", "DE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].countryCode", is("DE")))
                .andExpect(jsonPath("$[0].weekdayHolidayCount", is(12)));
    }

    @Test
    @DisplayName("GET /holidays/common?year=2025&countryA=US&countryB=DE return common list")
    void common_shouldReturnCommonHolidays() throws Exception {
        List<LastHolidayDTO> commons = List.of(
                new LastHolidayDTO(LocalDate.of(2025, 12, 25), "Christmas Day")
        );
        given(service.findCommonHolidays(2025, "US", "DE")).willReturn(commons);

        mvc.perform(get("/holidays/common")
                        .param("year",     "2025")
                        .param("countryA", "US")
                        .param("countryB", "DE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].date", is("2025-12-25")))
                .andExpect(jsonPath("$[0].name", is("Christmas Day")));
    }

    @Test
    @DisplayName("GET /holidays/last-three?country=BAD returns 400 Bad Request")
    void lastThree_invalidCountry_returnsBadRequest() throws Exception {
        given(service.getLastThreeHolidays("BAD"))
                .willThrow(new InvalidCountryCodeException("Invalid country code: BAD"));

        mvc.perform(get("/holidays/last-three")
                        .param("country", "BAD"))
                .andExpect(status().isBadRequest());
    }
}
