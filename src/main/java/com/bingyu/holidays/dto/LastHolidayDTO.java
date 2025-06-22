package com.bingyu.holidays.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LastHolidayDTO {

    private LocalDate date;

    private String name;

}
