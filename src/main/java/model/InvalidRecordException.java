package model;

/** A PIR field, value, or update is invalid. */
public final class InvalidRecordException extends IllegalArgumentException {
    public InvalidRecordException(String message) {
        super(message);
    }
}
