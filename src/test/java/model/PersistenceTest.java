package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Self-contained, automatically executable unit tests for US8-US11. */
public final class PersistenceTest {
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 10, 2, 9, 0);

    public static void main(String[] args) { TestSupport.runSuite(PersistenceTest.class); }

    /** US8: toDetailString of every PIR type contains the ID, type, and all fields. */
    private static void testDetailStringAllTypes() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Report", T2);
        Event event = model.createEvent("Meeting", T2, T1);
        Contact contact = model.createContact("Ada", "Hong Kong", "+852 1234 5678");

        check(note.toDetailString().contains("[NOTE] " + note.getId()), "note header");
        check(note.toDetailString().contains("Buy milk"), "note text");
        check(task.toDetailString().contains("[TASK] " + task.getId()), "task header");
        check(task.toDetailString().contains("Report"), "task description");
        check(event.toDetailString().contains("Meeting"), "event description");
        check(contact.toDetailString().contains("+852 1234 5678"), "contact mobile");
        check(task.toDetailString().contains(T2.toString()), "task deadline printed");
        check(event.toDetailString().contains(event.getId()) && event.toDetailString().contains("[EVENT]")
                && event.toDetailString().contains(T1.toString()) && event.toDetailString().contains(T2.toString()), "event fields printed");
        check(contact.toDetailString().contains(contact.getId()) && contact.toDetailString().contains("[CONTACT]")
                && contact.toDetailString().contains("Ada") && contact.toDetailString().contains("Hong Kong"), "contact fields printed");
    }

    /** US9: after deleting an existing record, the list omits it and getRecord fails. */
    private static void testDeleteExisting() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Report", T2);
        model.deleteRecord(note.getId());
        check(model.listRecords().equals(Arrays.<PIR>asList(task)), "only task remains");
        expect(RecordNotFoundException.class, () -> model.getRecord(note.getId()));
    }

    /** US9: deleting a missing ID throws. */
    private static void testDeleteMissing() {
        PIMModel model = new PIMModel();
        expect(RecordNotFoundException.class, () -> model.deleteRecord("missing"));
    }

    /** US9: the same ID can be added again after deletion. */
    private static void testDeleteThenReAdd() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Old");
        model.deleteRecord(note.getId());
        Note again = new Note(note.getId(), "New");
        model.addRecord(again);
        check(model.getRecord(note.getId()) == again, "same id re-added");
    }

    /** US10+US11: round trip preserves all types, IDs, and insertion order. */
    private static void testSaveLoadRoundTrip() throws Exception {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Report", T2);
        Event event = model.createEvent("Meeting", T2, T1);
        Contact contact = model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        Path file = tempPim("roundtrip.pim");

        PIMFileCodec.save(model, file);
        PIMModel loaded = PIMFileCodec.load(file);
        List<PIR> records = loaded.listRecords();

        check(records.size() == 4, "four records restored");
        check(records.get(0).getId().equals(note.getId())
                && ((Note) records.get(0)).getText().equals("Buy milk"), "note restored");
        check(records.get(1).getId().equals(task.getId())
                && ((Task) records.get(1)).getDeadline().equals(T2), "task restored");
        check(records.get(2).getId().equals(event.getId())
                && ((Event) records.get(2)).getAlarmTime().equals(T1), "event restored");
        check(records.get(3).getId().equals(contact.getId())
                && ((Contact) records.get(3)).getName().equals("Ada"), "contact restored");
        check(records.get(0).getType() == RecordType.NOTE
                && records.get(3).getType() == RecordType.CONTACT, "types restored");
        for (int i = 0; i < records.size(); i++) {
            check(records.get(i).toDetailString().equals(model.listRecords().get(i).toDetailString()), "all fields and IDs restored");
        }
    }

    /** US10+US11: newlines, Chinese text, and special symbols survive the round trip. */
    private static void testSaveLoadSpecialCharacters() throws Exception {
        PIMModel model = new PIMModel();
        model.createNote("line1\nline2 = with = and & symbols");
        model.createTask("\u4e2d\u6587 task & report", T2);
        Path file = tempPim("special.pim");

        PIMFileCodec.save(model, file);
        PIMModel loaded = PIMFileCodec.load(file);
        List<PIR> records = loaded.listRecords();

        check(records.size() == 2, "two records restored");
        check(((Note) records.get(0)).getText().equals("line1\nline2 = with = and & symbols"),
                "newlines and symbols intact");
        check(((Task) records.get(1)).getDescription().equals("\u4e2d\u6587 task & report"),
                "unicode intact");
    }

    /** US11: an empty file loads as an empty model. */
    private static void testLoadEmptyFile() throws Exception {
        Path file = tempPim("empty.pim");
        Files.write(file, new byte[0]);
        check(PIMFileCodec.load(file).listRecords().isEmpty(), "empty file is empty model");
    }

    /** US11: a missing file reports a clear error. */
    private static void testLoadMissingFile() throws Exception {
        Path file = tempPim("missing.pim");
        Files.deleteIfExists(file);
        expect(IOException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject UnknownType with the specified exception. */
    private static void testLoadUnknownType() throws Exception {
        Path file = tempPim("UnknownType.pim");
        Files.write(file, Arrays.asList("type=DIARY", "id=x", "text=QQ=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject DuplicateField with the specified exception. */
    private static void testLoadDuplicateField() throws Exception {
        Path file = tempPim("DuplicateField.pim");
        Files.write(file, Arrays.asList("type=NOTE", "id=x", "text=QQ==", "text=Qg=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject MissingField with the specified exception. */
    private static void testLoadMissingField() throws Exception {
        Path file = tempPim("MissingField.pim");
        Files.write(file, Arrays.asList("type=TASK", "id=x"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject InvalidTime with the specified exception. */
    private static void testLoadInvalidTime() throws Exception {
        Path file = tempPim("InvalidTime.pim");
        Files.write(file, Arrays.asList("type=TASK", "id=x", "description=QQ==", "deadline=2026-13-01 09:00"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject DuplicateId with the specified exception. */
    private static void testLoadDuplicateId() throws Exception {
        Path file = tempPim("DuplicateId.pim");
        Files.write(file, Arrays.asList("type=NOTE", "id=same", "text=QQ==", "", "type=NOTE", "id=same", "text=QQ=="));
        expect(DuplicateRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject MalformedLine without returning a partial model. */
    private static void testLoadMalformedLine() throws Exception {
        Path file = tempPim("MalformedLine.pim");
        Files.write(file, Arrays.asList("not-a-key-value"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject UnknownField without returning a partial model. */
    private static void testLoadUnknownField() throws Exception {
        Path file = tempPim("UnknownField.pim");
        Files.write(file, Arrays.asList("type=NOTE", "id=x", "text=QQ==", "extra=yes"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject MissingId without returning a partial model. */
    private static void testLoadMissingId() throws Exception {
        Path file = tempPim("MissingId.pim");
        Files.write(file, Arrays.asList("type=NOTE", "text=QQ=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject MissingType without returning a partial model. */
    private static void testLoadMissingType() throws Exception {
        Path file = tempPim("MissingType.pim");
        Files.write(file, Arrays.asList("id=x", "text=QQ=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject MissingDeadline without returning a partial model. */
    private static void testLoadMissingDeadline() throws Exception {
        Path file = tempPim("MissingDeadline.pim");
        Files.write(file, Arrays.asList("type=TASK", "id=x", "description=QQ=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** US11: reject InvalidBase64 without returning a partial model. */
    private static void testLoadInvalidBase64() throws Exception {
        Path file = tempPim("InvalidBase64.pim");
        Files.write(file, Arrays.asList("type=NOTE", "id=x", "text=@@"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(file));
    }

    /** Create a test file path in the system temp directory. */
    private static Path tempPim(String name) throws Exception {
        Path dir = Files.createTempDirectory("pimtest");
        return dir.resolve(name);
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    /** An action that may throw any exception, including checked ones. */
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static void expect(Class<? extends Throwable> type, ThrowingAction action) {
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
