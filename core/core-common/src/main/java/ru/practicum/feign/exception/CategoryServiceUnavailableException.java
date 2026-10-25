package ru.practicum.feign.exception;

public class CategoryServiceUnavailableException extends RuntimeException {
    public CategoryServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
