package ru.practicum.entity;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CompilationsParamDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.entity.QCompilation;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventFeignClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventFeignClient eventClient;

    @Override
    public List<CompilationDto> getAll(CompilationsParamDto dto) {

        int pageNumber = dto.getFrom() / dto.getSize();
        PageRequest pageRequest = PageRequest.of(pageNumber, dto.getSize());
        List<Compilation> compilations;
        if (dto.getPinned() == null) {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        } else {
            BooleanExpression condition = QCompilation.compilation.pinned.eq(dto.getPinned());
            compilations = compilationRepository.findAll(condition, pageRequest).getContent();
        }

        if (compilations.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = compilations.stream()
                .map(Compilation::getEventIds)
                .flatMap(Collection::stream)
                 .collect(Collectors.toSet());

        Map<Long, EventShortDto> events = eventClient.getByIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        EventShortDto::getId,
                        Function.identity()
                ));

        Map<Long, Set<EventShortDto>> compilationEventsMap = getCompilationEventsMap(compilations, events);

        return compilations.stream()
                .map(compilation ->
                        CompilationMapper.toDto(compilation, compilationEventsMap.get(compilation.getId())))
                .toList();
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = getByIdWithEvents(compId);

        Set<Long> eventIds = compilation.getEventIds();
        Set<EventShortDto> events = new HashSet<>(eventClient.getByIds(eventIds));

        return CompilationMapper.toDto(compilation, events);
    }

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {

        if (compilationRepository.existsByTitle(dto.getTitle())) {
            throw new ConflictException("Compilation with title " + dto.getTitle() + " already exists");
        }

        Set<EventShortDto> events = new HashSet<>(eventClient.getByIds(dto.getEvents()));
        Compilation entity = CompilationMapper.toEntity(dto);
        entity = compilationRepository.save(entity);

        var compilationDto = CompilationMapper.toDto(entity, events);
        log.info("Новая подборка событий: {}", CompilationMapper.toLog(compilationDto));

        return compilationDto;
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException(String.format("Compilation with id=%d not found", compId))
        );
        compilationRepository.delete(compilation);
        log.info("Удалена подборка событий id: {}", compilation.getId());
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getByIdWithEvents(compId);

        updateFields(compilation, request);

        Set<Long> eventIds = compilation.getEventIds();

        Set<EventShortDto> events = new HashSet<>(eventClient.getByIds(eventIds));
        var compilationDto = CompilationMapper.toDto(compilation, events);

        log.info("Подборка событий обновлена: {}", CompilationMapper.toLog(compilationDto));

        return compilationDto;
    }

    private Compilation getByIdWithEvents(Long compId) {
        return compilationRepository.findWithEventsById(compId).orElseThrow(
                () -> new NotFoundException(String.format("Compilation with id=%d not found", compId))
        );
    }

    private void updateFields(Compilation old, UpdateCompilationRequest request) {

        if (request.getTitle() != null) {
            if (!old.getTitle().equals(request.getTitle())) {
                if (compilationRepository.existsByTitle(request.getTitle())) {
                    throw new ConflictException(String.format("Compilation '%s' already exists", request.getTitle()));
                }

                old.setTitle(request.getTitle());
            }
        }

        if (request.getPinned() != null && !old.getPinned().equals(request.getPinned())) {
            old.setPinned(request.getPinned());
        }

        Set<Long> oldIds = old.getEventIds().stream()
                .collect(Collectors.toUnmodifiableSet());

        if (request.getEvents() != null && !oldIds.equals(request.getEvents())) {
            old.setEventIds(request.getEvents());
        }
    }

    Map<Long, Set<EventShortDto>> getCompilationEventsMap(List<Compilation> compilations,
                                                     Map<Long, EventShortDto> eventsMap) {

        Map<Long, Set<EventShortDto>> result = new HashMap<>();
        Set<EventShortDto> events;
        for (Compilation compilation : compilations) {
            events = compilation.getEventIds().stream()
                    .map(eventsMap::get)
                    .collect(Collectors.toSet());
            result.put(compilation.getId(), events);
        }
        return result;
    }
}
