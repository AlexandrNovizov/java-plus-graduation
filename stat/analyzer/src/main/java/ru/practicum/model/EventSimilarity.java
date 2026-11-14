package ru.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_similarity")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSimilarity {

    @EmbeddedId
    private EventSimilarityId id;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "calc_time", nullable = false)
    private LocalDateTime calcTime;
}
