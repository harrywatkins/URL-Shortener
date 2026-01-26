package uk.co.tpximpact.urlshortener.service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
