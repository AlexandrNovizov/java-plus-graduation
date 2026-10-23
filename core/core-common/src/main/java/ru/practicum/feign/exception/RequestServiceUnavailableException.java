package ru.practicum.feign.exception;

public class RequestServiceUnavailableException extends RuntimeException {
    public RequestServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
