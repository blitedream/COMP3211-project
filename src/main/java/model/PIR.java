package model;

/** Immutable base class for all personal information records. */
public abstract class PIR {
    private final String id;

    protected PIR(String id) {
        this.id = requireText(id, "id");
    }

    public final String getId() {
        return id;
    }

    public abstract RecordType getType();

    protected static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRecordException(field + " must be nonblank");
        }
        return value;
    }

    protected static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new InvalidRecordException(field + " is required");
        }
        return value;
    }
}
