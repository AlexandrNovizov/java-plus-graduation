package ru.practicum.service;

import ru.practicum.dto.stat.EndpointHit;
import ru.practicum.dto.stat.ParamDto;
import ru.practicum.dto.stat.ViewStats;

import java.util.List;

public interface StatService {

    void saveHit(EndpointHit dto);

    List<ViewStats> getStats(ParamDto paramDto);
}