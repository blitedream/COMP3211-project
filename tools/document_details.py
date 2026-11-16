"""Formal design and coverage content shared by the PDF build script."""
import re


def design(R, p, section, table, diagram, PageBreak):
    items = section('1 Architecture and dependencies',
        'The system uses MVC-inspired separation: PIMCli combines terminal view and controller, while the model package owns immutable records, validation, collection operations, search and persistence. This keeps domain behavior independent of terminal input and permits direct unit tests. A separate view class would add little value for this single terminal interface. The application uses Java standard libraries only; JaCoCo is a test tool.')
    items += [diagram('architecture')]
    items += section('2 Main component relationships',
        'PIMCli owns one replaceable PIMModel and calls PIMFileCodec directly for save/load. PIMModel aggregates zero or more PIRs in a LinkedHashMap<String,PIR> and delegates search to PIRSearch. Each record is one of Note, Task, Event or Contact, extending PIR. The codec reads public getters and reconstructs records through constructors; search reads immutable snapshots. Neither service depends on the CLI.')
    items += [diagram('components'), PageBreak()]
    items += section('3 Record inheritance and state',
        'The hollow triangle and shared connector below denote generalization towards PIR. PIR stores the private final String id, exposed through getId; subclasses provide immutable fields and detail formatting. Updating constructs and validates a replacement before storing it under the same ID. listRecords and search return immutable list snapshots in insertion order.')
    items += [diagram('classes')]
    items += section('3.1 Design decisions and alternatives',
        'MVC-inspired separation keeps command parsing in PIMCli and rules in model. Combining terminal view and controller avoids an extra forwarding layer; a second UI would justify splitting them. Immutable records prevent partial in-place edits: typed updates validate a complete replacement before one map insertion. This allocates a new record per update but makes rollback unnecessary. LinkedHashMap gives stable display order and expected constant-time exact-ID lookup; search and prefix resolution scan the collection. An index would add complexity without a demonstrated workload need.')
    items += section('3.2 Search and persistence boundaries',
        'Recursive descent expresses !, && and || precedence directly and parses once per search. Filtering is O(n times criterion evaluation cost), with O(n) result storage; ordered snapshots prevent callers changing the collection. A plain-text UTF-8 codec with Base64 fields avoids ambiguous delimiters without runtime dependencies. Its staging helper keeps file I/O outside PIMModel. Atomic replacement uses a temporary file on the same filesystem; unsupported providers fail safely rather than fall back to a destructive overwrite. Serializing and staging the whole collection costs O(serialized size) memory and temporary disk space. Concurrent writers, background alarms and power-loss durability are outside scope.')
    items += [PageBreak()]
    items += section('4 Search followed by update',
        'A user searches for tasks, selects an ID from the results, and changes its deadline. PIMCli sends the unchanged criterion string to PIMModel; PIRSearch parses it once and evaluates it over the snapshot. After a result is selected, the CLI resolves the full ID or a unique nonblank prefix, gets the current record, preserves unspecified fields and calls updateTask. The model validates the replacement before committing it. The diagram separates the successful replacement and validation-failure outcomes. A valid replacement is committed before the result returns; validation failure throws before storing it, and the CLI reports Error. An invalid criterion, unknown/ambiguous/blank ID or invalid date reports an error without modifying records.')
    items += [diagram('sequence'), PageBreak()]
    items += section('5 Fields, public/protected APIs and failures',
        'The following reference lists the top-level application classes and every explicitly declared public/protected constructor or method. Each signature includes argument names and types and its return type; constructors have no return type. Inherited Object methods are omitted. Static utility classes have private constructors and no public construction API. Exceptions listed include propagated runtime validation exceptions, not just checked throws declarations. Methods without a listed exception have no application-level exception contract for valid arguments.')
    metadata = {
      'PIR': ('private final String id.', 'Protected constructor validates a nonblank stable ID. getId returns it; getType and toDetailString are abstract contracts implemented by record types. requireText rejects null/blank text and returns the original value; requireValue rejects null and returns the original value.'),
      'Note': ('private final String text; inherited id.', 'Constructor stores nonblank note text. getText returns text, getType returns NOTE, and toDetailString includes ID, type and text.'),
      'Task': ('private final String description; private final LocalDateTime deadline; inherited id.', 'Constructor requires description and deadline. Getters return unchanged field values, getType returns TASK and detail output includes all fields.'),
      'Event': ('private final String description; private final LocalDateTime startTime, alarmTime; inherited id.', 'Constructor requires all three fields. Getters return them, getType returns EVENT and detail output includes all fields. Alarm is stored data; no background notification or alarm-before-start constraint is imposed.'),
      'Contact': ('private final String name, address, mobileNumber; inherited id.', 'Constructor requires all three nonblank fields. Getters return them, getType returns CONTACT and detail output includes all fields.'),
      'PIMModel': ('private final Map<String,PIR> records = new LinkedHashMap<String,PIR>().', 'Default constructor creates an empty collection; collection constructor imports records with stable IDs. addRecord rejects null/unsupported types and duplicates. create methods generate UUIDs and return stored records. getRecord retrieves by exact model ID. Typed updates retain ID/type and return validated replacements. deleteRecord returns the removed PIR. listRecords returns an immutable snapshot; search delegates to PIRSearch and returns an immutable matching snapshot.'),
      'PIRSearch': ('private static final DateTimeFormatter TIME_FORMAT; private parser/token/criterion types.', 'search validates and parses the complete criteria before evaluating records. It supports type equality, case-insensitive text contains, time < > =, parentheses, !, && and ||. Precedence is ! then && then ||. A field absent from a record evaluates false. Unknown fields/types, invalid dates, blank/null criteria, malformed tokens and null input records are rejected. No match returns an empty list. Search never mutates records.'),
      'PIMFileCodec': ('private static final DateTimeFormatter TIME_FORMAT; no mutable instance fields.', 'save writes all records as UTF-8 blocks with Base64 text and minute-precision times; load parses and validates every block before constructing a replacement model. IDs, field values at supported minute precision and insertion order survive round trips. Empty file means empty collection. Duplicate/unknown keys, invalid Base64, missing fields, invalid times/types and duplicate IDs fail. CLI enforces the .pim filename extension. Save serializes first, then calls package-private AtomicFileWriter to stage, flush and atomically replace. IOException includes unsupported atomic replacement; no destructive fallback is used.'),
      'PIMCli': ('private PIMModel model; private final PrintWriter out; private static final DateTimeFormatter TIME.', 'Constructor accepts an output writer. main opens UTF-8 terminal streams; run reads until EOF or quit. execute returns false for quit/exit and true otherwise. It reports expected argument, missing-ID, date and I/O errors as Error: and keeps the loop usable. Private resolveId rejects null/whitespace-only IDs before full-ID/prefix matching; exact matches take priority and ambiguous prefixes fail. Successful load replaces the collection only after decoding completes. quit/EOF do not save.'),
    }
    for name, (fields, behavior) in metadata.items():
        path = R/'src/main/java'/('cli' if name == 'PIMCli' else 'model')/(name+'.java')
        source = path.read_text(encoding='utf-8')
        # Public methods in PIMCli use one-line bodies; record/model signatures may wrap.
        pattern = r'^    ((?:public|protected)\s+(?:(?:static|final|abstract)\s+)*(?:<[^>]+>\s+)?[\w<>?,.\[\] ]+?\([^{};]*?\)(?:\s+throws\s+[\w, ]+)?)(?=\s*[;{])'
        signatures = [' '.join(m.group(1).split()) for m in re.finditer(pattern, source, re.M)]
        if name == 'PIRSearch': signatures = signatures[:1]  # private nested criterion implementations are internal
        rows = [['Signature (constructors have no return type)', 'Exceptions / return notes']]
        for sig in signatures:
            method = re.search(r'(\w+)\(', sig).group(1)
            exc = 'None for valid arguments.'
            if name in ['PIR','Note','Task','Event','Contact'] and method in [name,'requireText','requireValue']: exc = 'InvalidRecordException for a missing/blank required value.'
            if name == 'PIMModel':
                if method in ['PIMModel','addRecord']: exc = 'None.' if method == 'PIMModel' and 'PIMModel()' in sig else 'InvalidRecordException; DuplicateRecordException (import/add).'
                elif method.startswith('create'): exc = 'InvalidRecordException for invalid required values.'
                elif method.startswith('update'): exc = 'RecordNotFoundException; InvalidRecordException (wrong type/invalid values).'
                elif method in ['getRecord','deleteRecord']: exc = 'RecordNotFoundException for an unknown exact ID.'
                elif method == 'search': exc = 'InvalidSearchException; returns immutable List<PIR>.'
            if name == 'PIRSearch': exc = 'InvalidSearchException for null inputs/records or malformed criteria; returns immutable List<PIR>.'
            if name == 'PIMFileCodec': exc = 'IOException for filesystem failure.' + (' InvalidRecordException; DuplicateRecordException for invalid decoded data.' if method == 'load' else '')
            if name == 'PIMCli' and method in ['main','run']: exc = 'IOException for terminal I/O failure.'
            if name == 'PIMCli' and method == 'execute': exc = 'Expected command errors are caught and printed. Returns false only for quit/exit.'
            rows.append([sig, exc])
        items += section('5.'+str(list(metadata).index(name)+1)+' '+name, 'Fields: '+fields+'\nBehavior: '+behavior)
        if len(rows) < 2: raise ValueError('No APIs found for '+name)
        items += [table(rows, [300,211])]
    items += section('5.10 RecordType and exception types',
        'RecordType is a public enum with NOTE, TASK, EVENT and CONTACT; it has no explicitly declared public methods. InvalidRecordException, DuplicateRecordException and InvalidSearchException extend IllegalArgumentException; each declares public ClassName(String message), has no return type and passes message to its superclass. RecordNotFoundException extends NoSuchElementException and declares public RecordNotFoundException(String id), formatting the missing ID. These exception classes add no fields or other public/protected methods.')
    items += section('6 Integration choices and limits',
        'LocalDateTime has no timezone. CLI input and saved timestamps use yyyy-MM-dd HH:mm. Model clients should use minute precision to avoid losing seconds when saving. Text fields must be nonblank. Save uses atomic replacement on supported filesystems; staging failures preserve the old file. Power-loss recovery and concurrent writers are outside scope. CliTest checks command recovery and blank-ID rejection.')
    items += [PageBreak()]
    items += section('6.1 Internal atomic saving helper',
        'AtomicFileWriter is package-private, stateless and has no public/protected construction API. write(Path target, byte[] bytes) returns void and throws IOException. It creates a same-directory temporary file, writes every byte with FileChannel, calls force(true), closes the channel, then requests ATOMIC_MOVE with REPLACE_EXISTING. Failure deletes the temporary file; a cleanup IOException is suppressed on the original exception. Successful replacement consumes the staged file.')
    items += [diagram('saving')]
    items += section('6.2 Deterministic failure tests',
        'A package-private overload accepts Writer.write(Path path, byte[] bytes) and Replacer.replace(Path staged, Path target), both void operations throwing IOException. These per-call collaborators inject failures without mutable global hooks, disk exhaustion or OS-specific permissions. AtomicSaveTest compares original bytes after a partial staged write, failed replacement and unsupported atomic move. It checks staging cleanup, real successful replacement, an empty save and a directory target. The failed-load collection check belongs to CliTest, which owns the replacement of the active model.')
    items += section('6.3 Test organization and evidence',
        'JUnit 4.13.2 discovers public @Test methods and runs independent instances. NamedTestRunner uses JUnitCore with a listener to print each case name and PASS, FAIL or SKIP; failures do not stop subsequent cases, and any failure returns process status 1. Three separate reporter tests verify these outcomes; they are excluded from model counts and coverage. Search time-field/operator cases and malformed-file cases are independent. Related assertions verify one record or round-trip contract. Four model suites contain 56 cases; CLI checks run separately. Coverage uses the same compiled model classes, rejects mismatch warnings and uses relative agent paths for non-ASCII Windows directories.')
    return items


def coverage(p, section, table, PageBreak):
    data = [
      ('US1','Four record types and stable IDs','ModelCoreTest'),('US2','Create notes','ModelCoreTest; CliTest'),('US3','Create tasks/deadlines','ModelCoreTest; CliTest'),('US4','Create events/start/alarm','ModelCoreTest; CliTest'),('US5','Create contacts','ModelCoreTest; CliTest'),('US6','Update existing records','ModelCoreTest; CliTest'),('US7','Type, text, time and boolean search','SearchTest; CliTest'),('US8','Print one/all with all fields','PersistenceTest; CliTest'),('US9','Delete one record','PersistenceTest; CliTest'),('US10','Save all records to .pim','PersistenceTest; CliTest'),('US11','Load stored records','PersistenceTest; CliTest')]
    items = [p('US1-US11 and FR-01-FR-12 are implemented. The tables identify the code and tests for each requirement.'), table([['Story','Implemented behavior','Evidence']]+[list(row) for row in data],[60,245,206]), PageBreak()]
    fr = [
      ('FR-01','US1','Four types; stable unique IDs','RecordType; PIR; PIMModel.create*','ModelCoreTest'),
      ('FR-02','US2','Nonblank note text retained','Note; createNote','ModelCoreTest; CliTest'),
      ('FR-03','US3','Description/deadline retained','Task; createTask','ModelCoreTest; CliTest'),
      ('FR-04','US4','Description/start/alarm retained','Event; createEvent','ModelCoreTest; CliTest'),
      ('FR-05','US5','Name/address/mobile retained','Contact; createContact','ModelCoreTest; CliTest'),
      ('FR-06','US6','Partial updates; stable ID/type; invalid updates atomic','PIMModel.update*; PIMCli.update','ModelCoreTest; CliTest'),
      ('FR-07','US7','All required type/text/time comparisons','PIRSearch.search','SearchTest'),
      ('FR-08','US7','&&, ||, !, grouping; reject malformed criteria','PIRSearch parser','SearchTest; CliTest'),
      ('FR-09','US8','All fields of one/all PIRs; insertion order','PIR.toDetailString; print','PersistenceTest; CliTest'),
      ('FR-10','US9','Delete existing ID; unknown ID error','PIMModel.deleteRecord','PersistenceTest; CliTest'),
      ('FR-11','US10','Save all records, IDs/fields; staged atomic replacement','PIMFileCodec.save; AtomicFileWriter; CLI save','PersistenceTest; AtomicSaveTest; CliTest'),
      ('FR-12','US11','Restore IDs/order/fields; invalid load preserves memory','PIMFileCodec.load; CLI load','PersistenceTest; CliTest')]
    rows = [['FR / story','Status / behavior','Implementation / verification']]
    for rid,us,behavior,code,test in fr: rows.append([rid+' / '+us,'Implemented. '+behavior,code+'\n'+test])
    items += [p('Functional requirements','Heading1'),table(rows,[85,210,216]),PageBreak()]
    items += [p('Nonfunctional requirements','Heading1'),table([
      ['ID','Status','Implementation / verification'],
      ['NFR-01','Implemented.','The application uses Java standard libraries and targets Java 8 bytecode. Build and tests passed on Windows with Temurin JDK 21.0.3 and Azul Zulu JDK 8u512 (1.8.0_512-b09), 17 November 2026.'],
      ['NFR-02','Implemented.','All domain classes under model; four model suites run without CLI.'],
      ['NFR-03','Implemented for expected input/file failures.','PIMCli.execute catches expected errors. CliTest.testErrorRecoveryContract verifies the Error: prefix, nonempty reason, absence of a stack trace, identical records and a successful next command; other CLI cases cover dates, IDs and failed loads.'],
      ['NFR-04','Implemented.','NamedTestRunner prints each case name and outcome. Reporter tests verify failure reporting, continuation and skipped cases; a process probe confirms failure status 1. run_coverage.ps1 reproduces the model-only root Model_Line_Coverage.pdf measurement.']],[65,170,276])]
    items += section('Additional specified rules and limits',
        'Additional checks cover dates and required fields (ModelCoreTest, SearchTest, CliTest); blank or ambiguous IDs (CliTest); duplicate fields and IDs, malformed values and empty files (PersistenceTest); failed updates, failed loads and quit/EOF behavior (CliTest). AtomicSaveTest injects I/O failures and compares the original file bytes. Alarms are stored timestamps. Saved times have minute precision, and atomic replacement requires filesystem support.')
    return items
