package com.bingyu.holidays.client;

import com.bingyu.holidays.model.Holiday;

import java.util.List;

public interface HolidayApiClient {
    List<Holiday> fetchHolidays(int year, String countryCode);
}
