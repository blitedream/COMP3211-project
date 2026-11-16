# D: CLI and integration

## Developer guide

Model tests use JUnit 4.13.2 and Hamcrest 1.3 bundled under `lib/`. Add both JARs to the IDE test classpath. The application has no third-party runtime dependencies. Run `bash pim.sh test` on Linux or `test_all.ps1` on Windows.
Developed and tested on Windows with Temurin JDK 21.0.3; sources target Java 8. On 17 November 2026, the model, reporter and CLI checks also compiled and passed on Windows with Azul Zulu JDK 8u512 (1.8.0_512-b09).
Only standard Java libraries are used. Open the project folder in an IDE with Java support (for example IntelliJ IDEA), mark src/main/java as sources and src/test/java as tests, and run or debug cli.PIMCli.main.
From PowerShell in the repository root:

```powershell
.\run_pim.ps1
.\test_all.ps1
```
The first command compiles and launches the interactive app. The second compiles all sources and runs 56 model cases, three reporter checks and the CLI integration checks. No Maven or downloaded runtime dependencies are needed.
The Windows launcher enables ANSI output for the child process when supported,
passes the terminal width, and restores the previous console mode afterwards.
Use `run_pim.ps1 -Plain` for plain text. The Java entry point accepts `--plain`
and `--color`; setting `NO_COLOR` disables automatic color. The Bash launcher
also passes the available terminal width. ConsoleView handles formatting only.

## User guide
Enter commands at `pim>`, not directly in PowerShell. Commands and type names are case-insensitive. Quote arguments containing spaces with double quotes. Within quoted command arguments, two double quotes represent one literal quote. Backslashes are literal, including Windows paths. Dates use exactly `yyyy-MM-dd HH:mm`, with valid calendar dates and 24-hour times. Times have no timezone; model detail output uses ISO `T` between date and time.

```text
create note "Buy milk"
create task "Finish report" "2026-11-20 20:00"
create event "Group meeting" "2026-11-10 15:00" "2026-11-10 14:30"
create contact Alice "Hong Kong" 12345678
print all
search (type = task || type = event) && description contains "report"
save "my records.pim"
load "my records.pim"
help
quit
```
Creation assigns a short record number. Use `list` to see the current numbers.
The examples below assume that list shows a note as #1 and a task as #2; use the
numbers shown for your own records. `help` prints update examples with real
numbers from the current collection.

```text
list
print 1
update 1 text "New note text"
update 2 description "Revised task" deadline "2026-11-21 18:00"
delete 1
```
Both `2` and `#2` select record #2. Numbers remain attached to the same stored ID
through search, deletion and loading within this session; deleted numbers are
not reused for different IDs. Successful loading preserves known references and
assigns new numbers to newly encountered IDs. A failed load changes neither data
nor references. Numbers are not stored in the file, so use `list` again after
restarting. Original stable IDs and existing .pim files remain compatible.

Full IDs and unique non-numeric ID prefixes still work. Use `id:VALUE` to force
stored-ID lookup, including a numeric prefix. If a numeric stored ID conflicts
with a displayed number, the command is rejected; use `#NUMBER` or `id:VALUE`
to choose explicitly. Number 0, unavailable numbers and oversized numbers fail
without changing any records.

On supported terminals, the interface shows dark panels, colored success/error
messages, short references and an unsaved-changes indicator. `print` shows all
stored fields and the full ID; `list` is a compact overview. `clear` redraws the
welcome panel without changing records. Plain text remains available through
`run_pim.ps1 -Plain`. Save and successful load clear the unsaved indicator;
successful create/update/delete set it. Quit still does not save automatically.
Updates accept one or more field/value pairs and preserve unspecified fields, ID and type. Allowed fields: note: text; task: description, deadline; event: description, start, alarm; contact: name, address, mobile. Duplicate or inappropriate fields are rejected. Required text cannot be blank.

Print displays the ID, type and every stored field. Results start with the count; zero results is normal. Event alarms are stored data, not background notifications.

IDs must be nonblank. Both an empty ID (two quotes) and an ID containing only whitespace are rejected by print, update and delete, even when only one record exists. A nonempty prefix that matches several records is rejected; enter more characters or the full ID.

### Complete search reference

Keywords, field names, and type names are case-insensitive. Text matching is
also case-insensitive. A criteria string is a sequence of tokens separated by
whitespace. Text values and date/time values are double-quoted and may contain
spaces. A quoted value cannot contain a double quote. `!` may sit against the
next token (`!type`) or stand alone (`! type`).

```text
expression = or
or         = and { "||" and }
and        = not { "&&" not }
not        = "!" not | primary
primary    = "(" expression ")" | comparison
comparison = "type" "=" typeName
           | textField "contains" quoted
           | timeField ( "<" | ">" | "=" ) quoted
typeName   = "note" | "task" | "event" | "contact"
textField  = "text" | "description" | "name" | "address" | "mobile"
timeField  = "deadline" | "start" | "alarm"
quoted     = '"' characters '"'
```

`!` binds tighter than `&&`, and `&&` binds tighter than `||`. Parentheses
override that order. For example, `type = task || type = event && description
contains "report"` means `type = task || (type = event && description contains
"report")`.

Date/time values use exactly `yyyy-MM-dd HH:mm` inside quotes. `LocalDateTime`
has no timezone. This is the format the CLI and search module both use.

### Search examples

```text
type = task
text contains "milk"
deadline < "2026-10-02 09:00"
type = event && start > "2026-10-01 00:00"
(type = task || type = event) && description contains "report"
! type = note
```

## Field mapping

| Criterion name | Record types |
|---|---|
| `type` | all |
| `text` | Note |
| `description` | Task, Event |
| `name` | Contact |
| `address` | Contact |
| `mobile` | Contact |
| `deadline` | Task |
| `start` | Event |
| `alarm` | Event |

`contains` is true when the field value contains the quoted text, ignoring
case. `<`, `>`, and `=` compare `LocalDateTime` values. A comparison against a
field that the record does not have is false for that record. It is not an
error. `deadline < "2026-10-02 09:00"` therefore selects only tasks before that
instant.


### Search errors and output
Use `search CRITERIA` at the prompt; the grammar above describes CRITERIA. Examples above omit the leading search command. Put spaces around comparison and binary logical operators; parentheses and ! may touch adjacent tokens. Search quoted values cannot contain double quotes (the doubled-quote rule for create/update does not apply to search). Empty quoted search values, invalid dates, unknown fields/types, missing operands and unclosed parentheses print Error: and preserve all records. An otherwise valid criterion with no matches prints 0 record(s). Each match shows the complete record ID, type and fields, in insertion order, once per record.

```text
search type = contact && address contains "Hong Kong"
search deadline < "2026-11-20 20:00"
search !(type = note || type = contact)
```

Save stages all bytes in the destination directory, then atomically replaces the named .pim file. If staging or replacement fails, the old file is preserved; a filesystem without atomic replacement support reports an error instead of falling back to an unsafe overwrite. The destination directory must be writable. This does not guarantee recovery from power loss or disk corruption. Load replaces the entire current collection only after a successful read. Save first if current changes are needed. Relative paths resolve from the directory where you launched the program. Quit, exit and end-of-input do not automatically save. Invalid commands, arguments, dates, IDs and files print `Error:` and allow the next command; failed updates and failed loads preserve current data.

## Design
`cli.PIMCli` is the terminal controller and owns the session reference maps. Its public constructor accepts a PrintWriter for plain text; the package-private overload enables ConsoleView for ANSI formatting. `run(BufferedReader)` drives the loop and may throw IOException for terminal read failures; `execute(String)` returns false for exit and handles expected command errors. ConsoleView formats output and never mutates the model. The existing model owns all business rules, search and persistence. No A/B/C model files were changed.

```mermaid
sequenceDiagram
    actor User
    participant CLI as cli.PIMCli
    participant Model as model.PIMModel
    participant Search as model.PIRSearch
    User->>CLI: search criteria
    CLI->>Model: search(criteria)
    Model->>Search: search(records, criteria)
    Search-->>CLI: matching records via model
    CLI-->>User: IDs and details
    User->>CLI: update ID field value
    CLI->>Model: getRecord(ID)
    CLI->>Model: type-specific update with preserved fields
    Model-->>CLI: updated record
    CLI-->>User: Updated ID
```

## Validation and scope
D tests cover all four creation commands, partial note update, compound search, print, delete, save/load round trip, invalid updates, invalid dates and quoting, missing IDs, failed-load preservation, filename extension and EOF/quit. Existing model tests remain the authoritative domain tests. These are integration checks, not a new model coverage measurement. Existing coverage reports must be regenerated for the final submission as appropriate.
This Markdown file is development documentation; the course's final manuals/design documents still need DOC/DOCX/PDF export and group review. Demo video, presentation and honour declaration are separate group deliverables.
