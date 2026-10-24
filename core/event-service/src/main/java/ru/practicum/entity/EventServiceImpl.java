package ru.practicum.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.dto.util.CategoryUtil;
import ru.practicum.dto.util.UserUtil;
import ru.practicum.enums.event.AdminStateAction;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.event.SortBy;
import ru.practicum.enums.event.UserStateAction;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.CategoryFeignClient;
import ru.practicum.feign.RequestFeignClient;
import ru.practicum.feign.UserFeignClient;

import ru.practicum.entity.QEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static ru.practicum.dto.mapper.EventMapper.toLogEvent;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final StatClient statClient;
    private final EventRepository eventRepository;
    private final CategoryFeignClient categoryClient;
    private final RequestFeignClient requestClient;
    private final UserFeignClient userClient;

    private static final String APP_NAME = "ewm-main-service";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------- Private ----------

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        CategoryDto category = categoryClient.findById(dto.getCategory());

        UserFullDto user = userClient.findById(userId);

        Event event = EventMapper.mapToEntity(dto, userId);
        event = eventRepository.save(event);

        var result = EventMapper.toEventFullDto(event, 0L, 0L, user.getName(), category.getName());
        log.info("Новое событие: {}", toLogEvent(result));

        return result;
    }

    @Override
    public EventFullDto getById(Long userId, Long eventId) {


        Event event = getEvent(eventId);


        Map<Long, Long> viewsMap = getViewsMap(Collections.singletonList(event));
        Long views = viewsMap.getOrDefault(event.getId(), 0L);
        long confirmedRequests = getEventRequestsCount(eventId);

        UserFullDto user = userClient.findById(userId);
        CategoryDto category = categoryClient.findById(event.getCategoryId());

        return EventMapper.toEventFullDto(event, confirmedRequests, views, user.getName(), category.getName());
    }

    @Override
    public List<Event> getByIds(Collection<Long> ids) {
        return eventRepository.findAllById(ids);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        checkUserExist(userId);

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        UserFullDto initiator = userClient.findById(userId);
        Map<Long, String> initiatorMap = Map.of(initiator.getId(), initiator.getName());
        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> eventsRequestsCount = getEventsRequests(events);
        Map<Long, String> categoriesNamesMap = CategoryUtil.getCategoryNamesMap(categoryClient, categoryIds);

        return EventMapper.toEventShortDto(events, eventsRequestsCount, viewsMap, initiatorMap, categoriesNamesMap);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByCreatorId(EventParamDto eventParamDto, UpdateEventUserRequest request) {
        Long userId = eventParamDto.getUserId();
        UserFullDto author = userClient.findById(userId);
        Event event = getEvent(eventParamDto.getEventId());
        checkUserIsEventInitiator(userId, event);
        checkEventUpdateConditions(event, request);

        updateEventFromUserRequest(request, event);

        long requestsCount = getEventRequestsCount(event.getId());
        Long views = getViewsMap(Collections.singletonList(event)).getOrDefault(event.getId(), 0L);

        CategoryDto category = categoryClient.findById(event.getCategoryId());

        EventFullDto eventFullDto = EventMapper.toEventFullDto(event, requestsCount, views, author.getName(), category.getName());
        log.info("Событие обновлено создателем: {}", toLogEvent(eventFullDto));

        return eventFullDto;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByCreatorId(Long userId, Long eventId) {
        checkUserExist(userId);
        Event event = getEvent(eventId);
        checkUserIsEventInitiator(userId, event);

        return requestClient.getParticipationRequestsByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(EventParamDto eventParamDto,
                                                                    EventRequestStatusUpdateRequest request) {
        Long userId = eventParamDto.getUserId();
        Long eventId = eventParamDto.getEventId();
        Event event = getEvent(eventId);
        checkUserExist(userId);
        checkUserIsEventInitiator(userId, event);

        List<ParticipationRequestDto> requests = requestClient.findByIdsAndEventId(request.getRequestIds(), event.getId());


        long confirmedRequests = getEventRequestsCount(event.getId());
        long remainedRequests = event.getParticipantLimit() - confirmedRequests;

        RequestStatus targetStatus = request.getStatus();

        if (targetStatus == RequestStatus.CONFIRMED) {

            if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
                throw new ConflictException("Confirmation is not required or allowed for this event configuration.");
            }

            if (remainedRequests <= 0L) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequestDto req : requests) {
            if (!req.getStatus().equals(RequestStatus.PENDING.toString())) {
                throw new ConflictException(String.format("Request must have status PENDING. " +
                        "Request with id=" + req.getId() + " has status " + req.getStatus()));
            }

            if (targetStatus == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED.toString());
                rejected.add(req);
                continue;
            }

            if (remainedRequests <= 0L) {
                req.setStatus(RequestStatus.REJECTED.toString());
                rejected.add(req);
                continue;
            }
            req.setStatus(RequestStatus.CONFIRMED.toString());
            confirmed.add(req);
            remainedRequests--;
        }

        if (!confirmed.isEmpty()) {
            confirmed = requestClient.updateRequestsStatus(makeIdsSet(confirmed), RequestStatus.CONFIRMED);
        }
        if (!rejected.isEmpty()) {
            rejected = requestClient.updateRequestsStatus(makeIdsSet(rejected), RequestStatus.REJECTED);
        }


        var result = new EventRequestStatusUpdateResult(confirmed, rejected);
        log.info("Обновлены статусы заявок на событие: {}", result);

        return result;
    }

    // ---------- Admin ----------

    @Override
    public List<EventFullDto> getEventsByAdmin(EventAdminParamDto params) {
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        Predicate predicate = buildAdminPredicate(params);

        List<Event> events = predicate == null ?
                eventRepository.findAll(pageable).getContent() :
                eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        UserParamDto userParams = new UserParamDto();
        userParams.setIds(initiatorIds);
        List<UserDto> initiators = userClient.getUsers(userParams);

        Map<Long, String> initiatorsNamesMap = initiators.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> eventRequestsCount = getEventsRequests(events);
        Map<Long, String> categoriesNameMap = CategoryUtil.getCategoryNamesMap(categoryClient, categoryIds);

        return events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    Long requestsCount = eventRequestsCount.getOrDefault(event.getId(), 0L);
                    String initiatorName = initiatorsNamesMap.get(event.getId());
                    String categoryName = categoriesNameMap.get(event.getCategoryId());
                    return EventMapper.toEventFullDto(event, requestsCount, views, initiatorName, categoryName);
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEvent(eventId);

        UserFullDto initiator = userClient.findById(event.getInitiatorId());

        updateEventFromAdminRequest(request, event);

        applyStateAction(event, request.getStateAction());

        Map<Long, Long> viewsMap = getViewsMap(Collections.singletonList(event));
        Long views = viewsMap.getOrDefault(event.getId(), 0L);
        long confirmedRequests = getEventRequestsCount(event.getId());

        CategoryDto category = categoryClient.findById(event.getCategoryId());

        var result = EventMapper.toEventFullDto(event, confirmedRequests, views, initiator.getName(), category.getName());
        log.info("Событие обновлено админом: {}", toLogEvent(result));

        return result;
    }

    private void applyStateAction(Event event, AdminStateAction stateAction) {
        if (stateAction == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        switch (stateAction) {
            case AdminStateAction.PUBLISH_EVENT -> {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                }

                if (event.getEventDate().isBefore(now.plusHours(1))) {
                    throw new ConflictException("Cannot publish the event because event date is less than 1 hour from now.");
                }

                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now);
            }
            case AdminStateAction.REJECT_EVENT -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published.");
                }

                event.setState(EventState.CANCELED);
            }
            default -> throw new ConflictException("Invalid state action: " + stateAction);
        }
    }

    // ---------- Public ----------

    @Override
    public List<EventShortDto> getPublishedEvents(EventPublicParamDto params, HttpServletRequest request) {
        validateDateRange(params.getRangeStart(), params.getRangeEnd());

        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();
        SortBy sort = params.getSort();

        Pageable pageable = buildPageable(sort, params.getFrom(), params.getSize());
        Predicate predicate = buildPublicPredicate(params, rangeStart);

        List<Event> events = predicate == null ?
                eventRepository.findAll(pageable).getContent() :
                eventRepository.findAll(predicate, pageable).getContent();

        if (events.isEmpty()) {
            sendHit(request);
            return Collections.emptyList();
        }

        sendHit(request);

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> eventRequestsCount = getEventsRequests(events);

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, String> initiatorNames = UserUtil.getUserNamesMap(userClient, initiatorIds);
        Map<Long, String> categoryNames = CategoryUtil.getCategoryNamesMap(categoryClient, categoryIds);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            Long views = viewsMap.getOrDefault(event.getId(), 0L);
            Long requestsCount = eventRequestsCount.getOrDefault(event.getId(), 0L);
            String initiatorName = initiatorNames.get(event.getInitiatorId());
            String categoryName = categoryNames.get(event.getCategoryId());

            if (params.isOnlyAvailable()) {
                long remainedRequests = event.getParticipantLimit() == 0 ?
                        1 : event.getParticipantLimit() - requestsCount;
                if (remainedRequests > 0) {
                    result.add(EventMapper.toEventShortDto(event, requestsCount, views, initiatorName, categoryName));
                }
                continue;
            }
            result.add(EventMapper.toEventShortDto(event, requestsCount, views, initiatorName, categoryName));
        }

        if (SortBy.VIEWS == sort) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long id, HttpServletRequest request) {
        Event event = findEventByIdAndState(id, EventState.PUBLISHED);

        UserFullDto initiator = userClient.findById(event.getInitiatorId());

        sendHit(request);

        Map<Long, Long> viewsMap = getViewsMap(Collections.singletonList(event));
        Long views = viewsMap.getOrDefault(event.getId(), 0L);
        EventRequestsCountDto countDto = requestClient.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long confirmedRequests = countDto.count();

        CategoryDto category = categoryClient.findById(event.getCategoryId());

        return EventMapper.toEventFullDto(event, confirmedRequests, views, initiator.getName(), category.getName());
    }

    @Override
    public Map<Long, Long> getEventsRequests(List<Event> events) {
        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        List<EventRequestsCountDto> requests = requestClient.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);

        return requests.stream()
                .collect(Collectors.toMap(EventRequestsCountDto::eventId, EventRequestsCountDto::count));
    }

    @Override
    public Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        List<ViewStats> stats = statClient.getViewStats(new StatsRequest(
                start.format(formatter),
                LocalDateTime.now().plusYears(1).format(formatter),
                uris, true));

        return stats.stream()
                .collect(Collectors.toMap(
                        vs -> Long.parseLong(vs.getUri().substring("/events/".length())),
                        ViewStats::getHits));
    }

    @Override
    public Event findEventByIdAndState(Long id, EventState state) {
        return  eventRepository.findByIdAndState(id, state)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
    }

    @Override
    public boolean isEventExists(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    // ---------- Feign client ----------

    @Override
    public EventFullDto getByEventId(Long eventId) {
        Event event = getEvent(eventId);
        UserFullDto initiator = userClient.findById(event.getInitiatorId());

        Map<Long, Long> viewsMap = getViewsMap(Collections.singletonList(event));
        Long views = viewsMap.getOrDefault(event.getId(), 0L);
        EventRequestsCountDto countDto = requestClient.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long confirmedRequests = countDto.count();

        CategoryDto category = categoryClient.findById(event.getCategoryId());

        return EventMapper.toEventFullDto(event, confirmedRequests, views, initiator.getName(), category.getName());
    }

    @Override
    public List<EventShortDto> getByEventIds(Set<Long> ids) {

        List<Event> events = eventRepository.findAllById(ids);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, String> initiatorNames = UserUtil.getUserNamesMap(userClient, initiatorIds);
        Map<Long, Long> views = getViewsMap(events);
        Map<Long, Long> eventsRequestsCount = getEventsRequests(events);
        Map<Long, String> categoriesNames = CategoryUtil.getCategoryNamesMap(categoryClient, categoryIds);


        return EventMapper.toEventShortDto(events, eventsRequestsCount, views, initiatorNames, categoriesNames);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    // ---------- QueryDSL predicates ----------

    private Predicate buildAdminPredicate(EventAdminParamDto params) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            builder.and(event.initiatorId.in(params.getUsers()));
        }
        if (params.getStates() != null && !params.getStates().isEmpty()) {
            builder.and(event.state.in(params.getStates().stream()
                    .map(stateStr -> EventState.valueOf(stateStr.toUpperCase()))
                    .toList()));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            builder.and(event.categoryId.in(params.getCategories()));
        }
        if (params.getRangeStart() != null) {
            builder.and(event.eventDate.goe(params.getRangeStart()));
        }
        if (params.getRangeEnd() != null) {
            builder.and(event.eventDate.loe(params.getRangeEnd()));
        }
        return builder.getValue();
    }

    private Predicate buildPublicPredicate(EventPublicParamDto params, LocalDateTime rangeStart) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(event.state.eq(EventState.PUBLISHED));

        if (params.getText() != null && !params.getText().isBlank()) {
            String text = params.getText();
            builder.and(new BooleanBuilder()
                    .or(event.annotation.containsIgnoreCase(text))
                    .or(event.description.containsIgnoreCase(text)));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            builder.and(event.categoryId.in(params.getCategories()));
        }
        if (params.getPaid() != null) {
            builder.and(event.paid.eq(params.getPaid()));
        }
        if (rangeStart != null) {
            builder.and(event.eventDate.goe(rangeStart));
        }
        if (params.getRangeEnd() != null) {
            builder.and(event.eventDate.loe(params.getRangeEnd()));
        }
        return builder.getValue();
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new ValidationException("Range end must be after range start");
        }
    }

    private Pageable buildPageable(SortBy sort, int from, int size) {
        return SortBy.EVENT_DATE == sort ?
                PageRequest.of(from / size, size, Sort.by("eventDate").ascending())
                : PageRequest.of(from / size, size);
    }

    private void sendHit(HttpServletRequest request) {
        EndpointHit endpointHit = EndpointHit.builder()
                .app(APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(formatter))
                .build();
        statClient.hit(endpointHit);
        log.info("На сервер статистики отправлен endpointHit: {}", endpointHit);
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findWithRelationsById(eventId).orElseThrow(
                () -> new NotFoundException("Event with id " + eventId + " not found")
        );
    }

    private void updateEventFromUserRequest(UpdateEventUserRequest request, Event event) {
        ofNullable(request.getAnnotation()).ifPresent(event::setAnnotation);
        ofNullable(request.getDescription()).ifPresent(event::setDescription);
        ofNullable(request.getEventDate()).ifPresent(event::setEventDate);
        ofNullable(request.getLocationDto()).ifPresent(dto -> event.setLocation(LocationMapper.mapToEntity(dto)));
        ofNullable(request.getPaid()).ifPresent(event::setPaid);
        ofNullable(request.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        ofNullable(request.getRequestModeration()).ifPresent(event::setRequestModeration);
        ofNullable(request.getTitle()).ifPresent(event::setTitle);

        if (request.getCategory() != null) {
            event.setCategoryId(request.getCategory());
        }

        UserStateAction stateAction = request.getStateAction();

        if (stateAction == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        if (stateAction == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        }
    }

    public void updateEventFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event date cannot be in the past");
        }

        if (request.getCategory() != null) {
            event.setCategoryId(request.getCategory());
        }

        ofNullable(request.getAnnotation()).ifPresent(event::setAnnotation);
        ofNullable(request.getDescription()).ifPresent(event::setDescription);
        ofNullable(request.getEventDate()).ifPresent(event::setEventDate);
        ofNullable(request.getLocationDto()).ifPresent(dto -> event.setLocation(LocationMapper.mapToEntity(dto)));
        ofNullable(request.getPaid()).ifPresent(event::setPaid);
        ofNullable(request.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        ofNullable(request.getRequestModeration()).ifPresent(event::setRequestModeration);
        ofNullable(request.getTitle()).ifPresent(event::setTitle);
    }

    private void checkUserIsEventInitiator(Long userId, Event event) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(
                    String.format("User with id %d didn't initiate event with id %d", userId, event.getId())
            );
        }
    }

    private void checkEventUpdateConditions(Event event, UpdateEventUserRequest request) {
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null && !request.getEventDate().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours in the future.");

        }
    }

    private long getEventRequestsCount(Long eventId) {
        EventRequestsCountDto countDto = requestClient.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        return countDto.count();
    }

    private void checkUserExist(Long userId) {
        try {
            userClient.checkUserExists(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User with id: " + userId + " does not exist");
        }
    }

    private Set<Long> makeIdsSet(Collection<ParticipationRequestDto> dtos) {
        return dtos.stream()
                .map(ParticipationRequestDto::getId)
                .collect(Collectors.toSet());
    }
}