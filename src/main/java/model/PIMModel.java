package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** In-memory PIR collection. Search and persistence can use its public API. */
public final class PIMModel {
    private final Map<String, PIR> records = new LinkedHashMap<String, PIR>();

    public PIMModel() {
    }

    /** Construct a model from decoded PIRs; duplicate IDs are rejected. */
    public PIMModel(Collection<? extends PIR> initialRecords) {
        if (initialRecords == null) {
            throw new InvalidRecordException("initialRecords is required");
        }
        for (PIR record : initialRecords) {
            addRecord(record);
        }
    }

    /** Add a decoded PIR without changing its stable ID. */
    public PIR addRecord(PIR record) {
        if (record == null) {
            throw new InvalidRecordException("record is required");
        }
        if (!(record instanceof Note || record instanceof Task
                || record instanceof Event || record instanceof Contact)) {
            throw new InvalidRecordException("Unsupported PIR type");
        }
        if (records.containsKey(record.getId())) {
            throw new DuplicateRecordException("Record ID already exists: " + record.getId());
        }
        records.put(record.getId(), record);
        return record;
    }

    public Note createNote(String text) {
        Note record = new Note(newId(), text);
        addRecord(record);
        return record;
    }

    public Task createTask(String description, LocalDateTime deadline) {
        Task record = new Task(newId(), description, deadline);
        addRecord(record);
        return record;
    }

    public Event createEvent(String description, LocalDateTime startTime,
                             LocalDateTime alarmTime) {
        Event record = new Event(newId(), description, startTime, alarmTime);
        addRecord(record);
        return record;
    }

    public Contact createContact(String name, String address, String mobileNumber) {
        Contact record = new Contact(newId(), name, address, mobileNumber);
        addRecord(record);
        return record;
    }

    public PIR getRecord(String id) {
        PIR record = records.get(id);
        if (record == null) {
            throw new RecordNotFoundException(id);
        }
        return record;
    }

    /** Return an immutable snapshot in insertion order. */
    public List<PIR> listRecords() {
        return Collections.unmodifiableList(new ArrayList<PIR>(records.values()));
    }

    /** Replace a note while preserving its ID and type (US6). */
    public Note updateNote(String id, String text) {
        requireType(id, RecordType.NOTE);
        Note updated = new Note(id, text);
        records.put(id, updated);
        return updated;
    }

    /** Replace a task while preserving its ID and type (US6). */
    public Task updateTask(String id, String description, LocalDateTime deadline) {
        requireType(id, RecordType.TASK);
        Task updated = new Task(id, description, deadline);
        records.put(id, updated);
        return updated;
    }

    /** Replace an event while preserving its ID and type (US6). */
    public Event updateEvent(String id, String description, LocalDateTime startTime,
                             LocalDateTime alarmTime) {
        requireType(id, RecordType.EVENT);
        Event updated = new Event(id, description, startTime, alarmTime);
        records.put(id, updated);
        return updated;
    }

    /** Replace a contact while preserving its ID and type (US6). */
    public Contact updateContact(String id, String name, String address,
                                 String mobileNumber) {
        requireType(id, RecordType.CONTACT);
        Contact updated = new Contact(id, name, address, mobileNumber);
        records.put(id, updated);
        return updated;
    }

    private void requireType(String id, RecordType expected) {
        PIR current = getRecord(id);
        if (current.getType() != expected) {
            throw new InvalidRecordException("Record " + id + " is not a " + expected);
        }
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
