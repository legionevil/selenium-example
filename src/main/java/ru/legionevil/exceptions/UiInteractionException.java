package ru.legionevil.exceptions;

public class UiInteractionException extends RuntimeException {
    public UiInteractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UiInteractionException(String message) {
        super(message);
    }
}
