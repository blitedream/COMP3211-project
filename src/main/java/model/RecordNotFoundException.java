package model;

import java.util.NoSuchElementException;

/** No PIR has the requested ID. */
public final class RecordNotFoundException extends NoSuchElementException {
    public RecordNotFoundException(String id) {
        super("Record not found: " + id);
    }
}
