package ru.practicum.service;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.entity.Location;

@UtilityClass
public class LocationMapper {

    public static Location mapToEntity(LocationDto dto) {
        return Location.builder()
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public static LocationDto mapToDto(Location entity) {
        return LocationDto.builder()
                .lat(entity.getLat())
                .lon(entity.getLon())
                .build();
    }
}
