package model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Search tests for US7. */
public final class SearchTest {
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 10, 2, 9, 0);
    private static int passed;

    public static void main(String[] args) {
        testTypeFilter();
        testTextContains();
        testTimeComparison();
        testLogic();
        testMalformedCriteria();
        testModelSearch();
        testEdgeCases();
        System.out.println("Search tests passed: " + passed);
    }

    /** FR-07: type = note|task|event|contact keeps insertion order. */
    private static void testTypeFilter() {
        PIMModel model = new PIMModel();
        Note first = model.createNote("Buy milk");
        Task task = model.createTask("Finish report", T2);
        Event event = model.createEvent("Meeting", T2, T1);
        Contact contact = model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        Note second = model.createNote("Call Ada");
        List<PIR> records = model.listRecords();

        check(PIRSearch.search(records, "type = note")
                .equals(Arrays.<PIR>asList(first, second)), "notes in insertion order");
        check(PIRSearch.search(records, "TYPE = TASK")
                .equals(Arrays.<PIR>asList(task)), "task type is case-insensitive");
        check(PIRSearch.search(records, "type = event")
                .equals(Arrays.<PIR>asList(event)), "event type");
        check(PIRSearch.search(records, "type = contact")
                .equals(Arrays.<PIR>asList(contact)), "contact type");
        check(PIRSearch.search(records, "type = task").size() == 1, "one task");

        PIMModel notesOnly = new PIMModel();
        notesOnly.createNote("Only");
        check(PIRSearch.search(notesOnly.listRecords(), "type = contact").isEmpty(),
                "no match is an empty list");
        check(model.listRecords().equals(records), "search does not change the model");
        passed++;
    }

    /** FR-07: contains matches one text field, ignoring case, and skips other types. */
    private static void testTextContains() {
        PIMModel model = new PIMModel();
        Note milk = model.createNote("Buy milk");
        Note tea = model.createNote("Milk tea");
        Task task = model.createTask("Finish report", T2);
        Event event = model.createEvent("Meeting", T2, T1);
        Contact contact = model.createContact("Ada Lovelace", "Hong Kong", "+852 1234 5678");
        model.createNote("Call Ada");
        List<PIR> records = model.listRecords();

        check(PIRSearch.search(records, "text contains \"milk\"")
                .equals(Arrays.<PIR>asList(milk, tea)), "text substring in order");
        check(PIRSearch.search(records, "TEXT CONTAINS \"MiLk\"")
                .equals(Arrays.<PIR>asList(milk, tea)), "contains is case-insensitive");
        check(PIRSearch.search(records, "text contains \"buy milk\"")
                .equals(Arrays.<PIR>asList(milk)), "quoted text may contain spaces");
        check(PIRSearch.search(records, "description contains \"report\"")
                .equals(Arrays.<PIR>asList(task)), "task description");
        check(PIRSearch.search(records, "description contains \"meet\"")
                .equals(Arrays.<PIR>asList(event)), "event description");
        check(PIRSearch.search(records, "name contains \"love\"")
                .equals(Arrays.<PIR>asList(contact)), "contact name");
        check(PIRSearch.search(records, "address contains \"kong\"")
                .equals(Arrays.<PIR>asList(contact)), "contact address");
        check(PIRSearch.search(records, "mobile contains \"1234\"")
                .equals(Arrays.<PIR>asList(contact)), "contact mobile");
        check(PIRSearch.search(records, "text contains \"report\"").isEmpty(),
                "a task description is not note text");
        check(PIRSearch.search(records, "name contains \"Ada\"")
                .equals(Arrays.<PIR>asList(contact)), "note text is not a name");
        check(model.listRecords().equals(records), "contains search does not change the model");
        passed++;
    }

    /** FR-07: deadline, start, and alarm support <, >, and =. */
    private static void testTimeComparison() {
        PIMModel model = new PIMModel();
        model.createNote("Buy milk");
        Task early = model.createTask("Early", T1);
        model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        Task late = model.createTask("Late", T2);
        Event morning = model.createEvent("Morning", T1, T2);
        Event later = model.createEvent("Later", T2, T1);
        List<PIR> records = model.listRecords();

        check(PIRSearch.search(records, "deadline < \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(early)), "deadline before excludes equals");
        check(PIRSearch.search(records, "deadline > \"2026-10-01 09:00\"")
                .equals(Arrays.<PIR>asList(late)), "deadline after");
        check(PIRSearch.search(records, "DEADLINE = \"2026-10-01 09:00\"")
                .equals(Arrays.<PIR>asList(early)), "deadline equals");
        check(PIRSearch.search(records, "start < \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(morning)), "start before");
        check(PIRSearch.search(records, "start > \"2026-10-01 09:00\"")
                .equals(Arrays.<PIR>asList(later)), "start after");
        check(PIRSearch.search(records, "start = \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(later)), "start equals");
        check(PIRSearch.search(records, "alarm < \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(later)), "alarm is not the start time");
        check(PIRSearch.search(records, "alarm > \"2026-10-01 09:00\"")
                .equals(Arrays.<PIR>asList(morning)), "alarm after");
        check(PIRSearch.search(records, "alarm = \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(morning)), "alarm equals");
        check(PIRSearch.search(records, "deadline < \"2026-10-03 09:00\"")
                .equals(Arrays.<PIR>asList(early, late)), "time search keeps insertion order");
        expect(InvalidSearchException.class,
                () -> PIRSearch.search(records, "deadline < \"2026-13-01 09:00\""));
        expect(InvalidSearchException.class,
                () -> PIRSearch.search(records, "start = \"2026-10-01\""));
        check(model.listRecords().equals(records), "invalid time does not change the model");
        passed++;
    }

    /** FR-08: !, &&, and || combine criteria. && binds tighter than ||. */
    private static void testLogic() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task report = model.createTask("Finish report", T2);
        Task other = model.createTask("Buy milk", T1);
        Event meeting = model.createEvent("Meeting", T2, T1);
        Event reportEvent = model.createEvent("Status report", T1, T2);
        Contact contact = model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        List<PIR> records = model.listRecords();
        List<PIR> exceptNotes = Arrays.<PIR>asList(report, other, meeting, reportEvent, contact);

        check(PIRSearch.search(records, "type = task || type = event")
                .equals(Arrays.<PIR>asList(report, other, meeting, reportEvent)), "or keeps order");
        check(PIRSearch.search(records, "type = task && description contains \"report\"")
                .equals(Arrays.<PIR>asList(report)), "and requires both sides");
        check(PIRSearch.search(records, "type = task && description contains \"nope\"").isEmpty(),
                "and can match nothing");
        check(PIRSearch.search(records, "! type = note").equals(exceptNotes), "not type");
        check(PIRSearch.search(records, "!type = note").equals(exceptNotes), "bang may be attached");
        check(PIRSearch.search(records, "!(type = note)").equals(exceptNotes), "not with parentheses");
        check(PIRSearch.search(records, "!! type = note").equals(Arrays.<PIR>asList(note)),
                "double negation");
        check(PIRSearch.search(records,
                "type = task || type = event && description contains \"report\"")
                .equals(Arrays.<PIR>asList(report, other, reportEvent)),
                "and binds tighter than or");
        check(PIRSearch.search(records,
                "(type = task || type = event) && description contains \"report\"")
                .equals(Arrays.<PIR>asList(report, reportEvent)),
                "parentheses override precedence");
        check(PIRSearch.search(records, "!(type = note || type = contact)")
                .equals(Arrays.<PIR>asList(report, other, meeting, reportEvent)),
                "not over a grouped or");
        check(model.listRecords().equals(records), "logic search does not change the model");
        passed++;
    }

    /** FR-08: malformed criteria fail clearly, and no match is an empty list. */
    private static void testMalformedCriteria() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        List<PIR> records = model.listRecords();

        expect(InvalidSearchException.class, () -> PIRSearch.search(null, "type = note"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, null));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, ""));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "   "));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "color contains \"red\""));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "type = person"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "text contains \"\""));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "text contains \"milk"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "type ="));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "type = note &&"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "!"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "type = note extra"));
        expect(InvalidSearchException.class, () -> PIRSearch.search(records, "(type = note"));
        expect(InvalidSearchException.class,
                () -> PIRSearch.search(records, "deadline < \"yesterday\""));
        check(PIRSearch.search(records, "type = contact").isEmpty(),
                "no match returns an empty list");
        check(model.listRecords().equals(records), "malformed search does not change the model");
        check(model.getRecord(note.getId()) == note, "the stored note is the same object");
        passed++;
    }

    /** CLI calls PIMModel.search, which delegates to PIRSearch. */
    private static void testModelSearch() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Buy milk");
        Task task = model.createTask("Finish report", T2);
        model.createEvent("Meeting", T2, T1);
        List<PIR> before = model.listRecords();

        check(model.search("type = note && text contains \"milk\"")
                .equals(Arrays.<PIR>asList(note)), "model search combines criteria");
        check(model.search("deadline = \"2026-10-02 09:00\"")
                .equals(Arrays.<PIR>asList(task)), "model search reads stored fields");
        check(model.search("type = contact").isEmpty(), "model search returns an empty list");
        expect(InvalidSearchException.class, () -> model.search("type ="));
        check(model.listRecords().equals(before), "model search does not change stored records");
        passed++;
    }

    /** Spec edges: updates, one hit per record, invalid dates, and grouping. */
    private static void testEdgeCases() {
        PIMModel model = new PIMModel();
        Note note = model.createNote("Old text");
        model.updateNote(note.getId(), "New milk");
        Task reportTask = model.createTask("status report", T1);
        Task otherTask = model.createTask("Buy milk", T2);
        Event reportEvent = model.createEvent("report meeting", T2, T1);
        Event otherEvent = model.createEvent("Lunch", T1, T2);
        model.createContact("Ada", "Hong Kong", "+852 1234 5678");
        List<PIR> records = model.listRecords();

        List<PIR> updated = model.search("text contains \"milk\"");
        check(updated.size() == 1 && ((Note) updated.get(0)).getText().equals("New milk"),
                "search sees the updated note");
        check(model.search("text contains \"Old\"").isEmpty(), "search ignores the replaced text");
        expect(UnsupportedOperationException.class, () -> updated.clear());

        check(model.search("description contains \"report\"")
                .equals(Arrays.<PIR>asList(reportTask, reportEvent)),
                "description matches task and event in order");
        check(model.search("type = note || text contains \"milk\"")
                .equals(updated), "a record that matches both sides appears once");
        check(model.search("! type = task && description contains \"report\"")
                .equals(Arrays.<PIR>asList(reportEvent)),
                "not binds tighter than and");
        check(model.search("deadline < \"2026-10-02 09:00\" || start = \"2026-10-01 09:00\"")
                .equals(Arrays.<PIR>asList(reportTask, otherEvent)),
                "time criteria can be combined");
        check(model.search("deadline < \"2026-10-03 09:00\" && type = note").isEmpty(),
                "a note has no deadline");
        check(new PIMModel().search("type = note").isEmpty(), "empty model returns an empty list");

        Note imported = new Note("saved-id", "Imported milk");
        PIMModel loaded = new PIMModel(Arrays.<PIR>asList(imported));
        check(loaded.search("text contains \"imported\"").get(0) == imported,
                "search finds an imported record");

        expect(InvalidSearchException.class,
                () -> model.search("deadline = \"2026-02-29 09:00\""));
        expect(InvalidSearchException.class,
                () -> model.search("start = \"2026-04-31 09:00\""));
        expect(InvalidSearchException.class, () -> model.search("type = note)"));
        expect(InvalidSearchException.class, () -> model.search(""));
        expect(InvalidSearchException.class, () -> new PIMModel().search("   "));
        expect(InvalidSearchException.class,
                () -> PIRSearch.search(Arrays.<PIR>asList((PIR) null), "type = note"));
        check(model.listRecords().equals(records), "edge searches do not change the model");
        check(otherTask.getDescription().equals("Buy milk"), "stored task text stays intact");
        check(model.search("text contains \"a && b\"").isEmpty(),
                "operators inside quotes are literal text");
        Note both = model.createNote("a && b");
        check(model.search("(type = note || type = task) && text contains \"a && b\"")
                .equals(Arrays.<PIR>asList(both)),
                "nested groups keep quoted operators literal");
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
