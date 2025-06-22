package com.bingyu.holidays.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CountryHolidayCount {

    private String countryCode;

    private int weekdayHolidayCount;

}
