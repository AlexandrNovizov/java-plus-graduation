package ru.practicum.service;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.LogCompilation;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.mapper.EventMapper;
import ru.practicum.entity.Compilation;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public static CompilationDto toDto(Compilation entity,
                                       Set<EventShortDto> events) {
        return CompilationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .pinned(entity.getPinned())
                .events(events)
                .build();
    }

    public static LogCompilation toLog(CompilationDto compilationDto) {
        return LogCompilation.builder()
                .id(compilationDto.getId())
                .title(compilationDto.getTitle())
                .pinned(compilationDto.getPinned())
                .events(compilationDto.getEvents().stream()
                        .map(EventMapper::toLogEventShort)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static Compilation toEntity(NewCompilationDto dto) {
        Compilation entity = new Compilation();

        Optional.ofNullable(dto.getPinned()).ifPresent(entity::setPinned);

        entity.setEventIds(dto.getEvents());

        entity.setTitle(dto.getTitle());

        return entity;
    }
}
