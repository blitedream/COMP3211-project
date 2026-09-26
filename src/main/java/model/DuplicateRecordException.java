package model;

/** A PIR with this ID is already present. */
public final class DuplicateRecordException extends IllegalArgumentException {
    public DuplicateRecordException(String message) {
        super(message);
    }
}
