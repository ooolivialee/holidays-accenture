package com.bingyu.holidays.controller;

import com.bingyu.holidays.dto.CountryHolidayCount;
import com.bingyu.holidays.dto.LastHolidayDTO;
import com.bingyu.holidays.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/holidays")
@Validated
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService service;

    @GetMapping("/last-three")
    public ResponseEntity<List<LastHolidayDTO>> lastThree(
            @RequestParam String country) {
        return ResponseEntity.ok(service.getLastThreeHolidays(country));
    }

    @GetMapping("/weekday-count")
    public ResponseEntity<List<CountryHolidayCount>> weekdayCount(
            @RequestParam int year,
            @RequestParam List<String> countries) {
        return ResponseEntity.ok(service.countWeekdayHolidays(year, countries));
    }

    @GetMapping("/common")
    public ResponseEntity<List<LastHolidayDTO>> common(
            @RequestParam int year,
            @RequestParam String countryA,
            @RequestParam String countryB) {
        return ResponseEntity.ok(service.findCommonHolidays(year, countryA, countryB));
    }
}
