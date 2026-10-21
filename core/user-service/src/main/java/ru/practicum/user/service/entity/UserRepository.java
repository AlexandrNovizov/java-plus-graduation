package ru.practicum.user.service.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByMail(String email);

    List<User> findAllByIdIn(List<Long> ids);
}
