package model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Self-contained, automatically executable unit tests for US1-US6. */
public final class ModelCoreTest {
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 10, 2, 9, 0);
    private static int passed;

    public static void main(String[] args) {
        testCreateAllTypes();
        testRejectInvalidCreation();
        testUpdateAllTypes();
        testUpdateFailureIsAtomic();
        testImportedIdsAndSnapshots();
        testMissingAndWrongType();
        System.out.println("Model core tests passed: " + passed);
    }

    /** US1-US5: all four PIR types retain required data and distinct IDs. */
    private static void testCreateAllTypes() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Finish report", T2);
        Event event = model.createEvent("Meeting", T2, T1);
        Contact contact = model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        check(note.getType() == RecordType.NOTE, "note type");
        check(task.getType() == RecordType.TASK && task.getDeadline().equals(T2), "task fields");
        check(event.getType() == RecordType.EVENT && event.getAlarmTime().equals(T1), "event fields");
        check(contact.getType() == RecordType.CONTACT
                && contact.getMobileNumber().equals("+852 1234 5678"), "contact fields");
        check(new HashSet<String>(Arrays.asList(note.getId(), task.getId(),
                event.getId(), contact.getId())).size() == 4, "unique IDs");
        check(model.listRecords().equals(Arrays.<PIR>asList(note, task, event, contact)),
                "insertion order");
        passed++;
    }

    /** US2-US5: invalid required fields do not add a PIR. */
    private static void testRejectInvalidCreation() {
        PIMModel model = new PIMModel();
        expect(InvalidRecordException.class, () -> model.createNote("  "));
        expect(InvalidRecordException.class, () -> model.createTask("Task", null));
        expect(InvalidRecordException.class, () -> model.createEvent("", T2, T1));
        expect(InvalidRecordException.class,
                () -> model.createContact("Ada", "", "123"));
        check(model.listRecords().isEmpty(), "invalid records were not added");
        passed++;
    }

    /** US6: each PIR can be modified without changing its type or ID. */
    private static void testUpdateAllTypes() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Old");
        Task task = model.createTask("Old", T1);
        Event event = model.createEvent("Old", T1, T1);
        Contact contact = model.createContact("Ada", "Old", "123");
        check(model.updateNote(note.getId(), "New").getText().equals("New"), "note update");
        check(model.updateTask(task.getId(), "New", T2).getDeadline().equals(T2),
                "task update");
        check(model.updateEvent(event.getId(), "New", T2, T1).getStartTime().equals(T2),
                "event update");
        check(model.updateContact(contact.getId(), "Grace", "New", "456")
                .getName().equals("Grace"), "contact update");
        check(model.getRecord(task.getId()).getId().equals(task.getId()), "stable ID");
        check(model.getRecord(task.getId()).getType() == RecordType.TASK, "stable type");
        check(task.getDescription().equals("Old"), "original object is immutable");
        passed++;
    }

    /** US6: a rejected update leaves the existing PIR unchanged. */
    private static void testUpdateFailureIsAtomic() {
        PIMModel model = new PIMModel();
        Event event = model.createEvent("Meeting", T2, T1);
        expect(InvalidRecordException.class,
                () -> model.updateEvent(event.getId(), "", T2, T1));
        expect(InvalidRecordException.class,
                () -> model.updateEvent(event.getId(), "New", T2, null));
        check(model.getRecord(event.getId()) == event, "failed update did not replace record");
        passed++;
    }

    /** Stable IDs support future .pim loading; snapshots cannot mutate the model. */
    private static void testImportedIdsAndSnapshots() {
        Note imported = new Note("saved-id", "Imported");
        PIMModel model = new PIMModel(Arrays.<PIR>asList(imported));
        check(model.getRecord("saved-id") == imported, "imported ID retained");
        expect(DuplicateRecordException.class, () -> model.addRecord(imported));
        List<PIR> snapshot = model.listRecords();
        expect(UnsupportedOperationException.class, () -> snapshot.clear());
        check(model.listRecords().size() == 1, "model remained unchanged");
        passed++;
    }

    /** Unknown IDs and updates through the wrong PIR type report clear errors. */
    private static void testMissingAndWrongType() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Text");
        expect(RecordNotFoundException.class, () -> model.getRecord("missing"));
        expect(RecordNotFoundException.class,
                () -> model.updateNote("missing", "New"));
        expect(InvalidRecordException.class,
                () -> model.updateTask(note.getId(), "Wrong", T1));
        check(model.getRecord(note.getId()) == note, "wrong-type update is atomic");
        passed++;
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Expected " + type.getSimpleName()
                    + " but got " + actual.getClass().getSimpleName(), actual);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
