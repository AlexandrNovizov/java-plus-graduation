package ru.practicum.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserFullDto {

    private String email;
    private Long id;
    private String name;
    private Integer adminWarnings;
    private String bannedUntil;
    private Integer commentsCount = 0;
    private String rank;
}
