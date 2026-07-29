package fr.afpa.gestioneleves.exception;

public class PhotoTooLargeException extends RuntimeException {
    public PhotoTooLargeException(String message) {
        super(message);
    }
}
