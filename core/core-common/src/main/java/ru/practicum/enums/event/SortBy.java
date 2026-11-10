package ru.practicum.enums.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SortBy {
    EVENT_DATE("eventDate"), RATING("rating");

    private final String databaseRowName;
}
