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
    private static int passed;

    public static void main(String[] args) throws Exception {
        testDetailStringAllTypes();
        testDeleteExisting();
        testDeleteMissing();
        testDeleteThenReAdd();
        testSaveLoadRoundTrip();
        testSaveLoadSpecialCharacters();
        testLoadEmptyFile();
        testLoadMissingFile();
        testLoadErrors();
        System.out.println("Persistence tests passed: " + passed);
    }

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
        passed++;
    }

    /** US9: after deleting an existing record, the list omits it and getRecord fails. */
    private static void testDeleteExisting() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Report", T2);
        model.deleteRecord(note.getId());
        check(model.listRecords().equals(Arrays.<PIR>asList(task)), "only task remains");
        expect(RecordNotFoundException.class, () -> model.getRecord(note.getId()));
        passed++;
    }

    /** US9: deleting a missing ID throws. */
    private static void testDeleteMissing() {
        PIMModel model = new PIMModel();
        expect(RecordNotFoundException.class, () -> model.deleteRecord("missing"));
        passed++;
    }

    /** US9: the same ID can be added again after deletion. */
    private static void testDeleteThenReAdd() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Old");
        model.deleteRecord(note.getId());
        Note again = new Note(note.getId(), "New");
        model.addRecord(again);
        check(model.getRecord(note.getId()) == again, "same id re-added");
        passed++;
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
        passed++;
    }

    /** US10+US11: newlines, Chinese text, and special symbols survive the round trip. */
    private static void testSaveLoadSpecialCharacters() throws Exception {
        PIMModel model = new PIMModel();
        model.createNote("line1\nline2 = with = and & symbols");
        model.createTask("Chinese task & report", T2);
        Path file = tempPim("special.pim");

        PIMFileCodec.save(model, file);
        PIMModel loaded = PIMFileCodec.load(file);
        List<PIR> records = loaded.listRecords();

        check(records.size() == 2, "two records restored");
        check(((Note) records.get(0)).getText().equals("line1\nline2 = with = and & symbols"),
                "newlines and symbols intact");
        check(((Task) records.get(1)).getDescription().equals("Chinese task & report"),
                "unicode intact");
        passed++;
    }

    /** US11: an empty file loads as an empty model. */
    private static void testLoadEmptyFile() throws Exception {
        Path file = tempPim("empty.pim");
        Files.write(file, new byte[0]);
        check(PIMFileCodec.load(file).listRecords().isEmpty(), "empty file is empty model");
        passed++;
    }

    /** US11: a missing file reports a clear error. */
    private static void testLoadMissingFile() throws Exception {
        Path file = tempPim("missing.pim");
        Files.deleteIfExists(file);
        expect(IOException.class, () -> PIMFileCodec.load(file));
        passed++;
    }

    /** US11: unknown type, missing field, bad time, and duplicate ID all fail clearly and leave memory unchanged. */
    private static void testLoadErrors() throws Exception {
        Path unknown = tempPim("unknown.pim");
        Files.write(unknown, Arrays.asList("type=DIARY", "id=x", "text=QQ=="));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(unknown));

        Path missing = tempPim("missing.pim");
        Files.write(missing, Arrays.asList("type=TASK", "id=x"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(missing));

        Path badTime = tempPim("badtime.pim");
        Files.write(badTime, Arrays.asList(
                "type=TASK", "id=x", "description=QQ==", "deadline=2026-13-01 09:00"));
        expect(InvalidRecordException.class, () -> PIMFileCodec.load(badTime));

        Path dup = tempPim("dup.pim");
        Files.write(dup, Arrays.asList(
                "type=NOTE", "id=same", "text=QQ==", "",
                "type=NOTE", "id=same", "text=QQ=="));
        expect(DuplicateRecordException.class, () -> PIMFileCodec.load(dup));

        PIMModel model = new PIMModel();
        Note note = model.createNote("Still here");
        expect(InvalidRecordException.class,
                () -> PIMFileCodec.load(unknown));
        check(model.getRecord(note.getId()) == note, "failed load keeps memory intact");
        passed++;
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
