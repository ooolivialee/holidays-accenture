package com.bingyu.holidays.dto;

import com.bingyu.holidays.model.Holiday;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HolidayMapper {

    LastHolidayDTO toLastHolidayDTO(Holiday holiday);

    List<LastHolidayDTO> toLastHolidayDTOs(List<Holiday> holidays);
}
