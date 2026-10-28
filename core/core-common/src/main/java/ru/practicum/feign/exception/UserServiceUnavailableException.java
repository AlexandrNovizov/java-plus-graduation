package ru.practicum.feign.exception;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(String message, Throwable cause) {
        super(message);
    }
}
