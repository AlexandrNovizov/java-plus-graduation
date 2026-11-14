package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAction {

    @EmbeddedId
    private UserActionId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private UserActionType actionType;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;
}
