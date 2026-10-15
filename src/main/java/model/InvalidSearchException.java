package model;

/** A search criteria string is malformed. */
public final class InvalidSearchException extends IllegalArgumentException {
    public InvalidSearchException(String message) {
        super(message);
    }
}
