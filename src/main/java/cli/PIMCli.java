package cli;

import model.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Terminal adapter: all domain operations are delegated to the model. */
public final class PIMCli {
    private PIMModel model = new PIMModel();
    private final PrintWriter out;
    private final ConsoleView view;
    private boolean unsaved;
    private final Map<String, Integer> numbers = new LinkedHashMap<String, Integer>();
    private final Map<Integer, String> idsByNumber = new LinkedHashMap<Integer, String>();
    private int nextNumber = 1;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public PIMCli(PrintWriter out) { this(out, false); }
    PIMCli(PrintWriter out, boolean color) {
        this.out = out;
        this.view = color ? new ConsoleView(out) : null;
    }
    public static void main(String[] args) throws IOException {
        boolean color = System.console() != null && System.getenv("NO_COLOR") == null
                && !"dumb".equals(System.getenv("TERM"));
        for (String arg : args) {
            if (arg.equals("--plain")) color = false;
            else if (arg.equals("--color")) color = true;
            else throw new IllegalArgumentException("Usage: cli.PIMCli [--plain|--color]");
        }
        PIMCli cli = new PIMCli(new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true), color);
        cli.run(new BufferedReader(new InputStreamReader(System.in, "UTF-8")));
    }
    public void run(BufferedReader input) throws IOException {
        if (view != null) view.welcome();
        else welcomePlain();
        try {
            String line;
            while (true) {
                if (view != null) view.prompt(model.listRecords().size(), unsaved);
                else { out.print("pim> "); out.flush(); }
                line = input.readLine();
                if (line == null || !execute(line)) break;
            }
        } finally {
            if (view != null) { out.print(ConsoleView.RESET); out.flush(); }
        }
    }
    private void welcomePlain() {
        out.println("============================================================");
        out.println("  PIM | Personal Information Manager");
        out.println("  Notes - Tasks - Events - Contacts");
        out.println("============================================================");
        out.println("Type commands at pim>. Quick start:");
        out.println("  create note \"Buy milk\"");
        out.println("  print all");
        out.println("  list     (show short record numbers)");
        out.println("  print 1  (use a number from list)");
        out.println("  search type = note");
        out.println("  save \"my-records.pim\"");
        out.println("Type help for all commands, or quit to exit.");
        out.println("Save before quitting: changes are not saved automatically.");
        out.println("To resume saved work: load \"my-records.pim\"");
        out.println();
    }
    /** Execute one command; false requests exit. Expected errors leave the loop usable. */
    public boolean execute(String line) {
        try {
            line = line.trim();
            if (line.isEmpty()) return true;
            String[] head = line.split("\\s+", 2);
            String command = head[0].toLowerCase(Locale.ROOT);
            if (command.equals("search")) {
                show(model.search(head.length == 2 ? head[1] : "")); return true;
            }
            List<String> a = tokens(line);
            switch (command) {
                case "help": count(a, 1); help(); break;
                case "list": count(a, 1); list(); break;
                case "clear": count(a, 1); if (view != null) view.welcome(); else welcomePlain(); break;
                case "quit": case "exit":
                    count(a, 1);
                    if (view != null && unsaved) view.hint("Unsaved changes were not written to a file.");
                    out.println("Goodbye."); out.flush(); return false;
                case "create": create(a); break;
                case "update": update(a); break;
                case "print":
                    count(a, 2);
                    if (a.get(1).equalsIgnoreCase("all")) show(model.listRecords());
                    else showOne(model.getRecord(resolveId(a.get(1))));
                    break;
                case "delete": {
                    count(a, 2);
                    String id = resolveId(a.get(1));
                    int number = numberOf(model.getRecord(id));
                    model.deleteRecord(id);
                    unsaved = true;
                    if (view != null) view.message("Deleted record #" + number + ".");
                    else out.println("Deleted " + id);
                    break;
                }
                case "save":
                    count(a, 2); PIMFileCodec.save(model, file(a.get(1))); unsaved = false;
                    if (view != null) view.message("Saved " + model.listRecords().size() + " record(s) to " + a.get(1) + ".");
                    else out.println("Saved.");
                    break;
                case "load":
                    count(a, 2);
                    PIMModel loaded = PIMFileCodec.load(file(a.get(1)));
                    model = loaded;
                    unsaved = false;
                    for (PIR record : model.listRecords()) numberOf(record);
                    if (view != null) {
                        view.message("Loaded " + model.listRecords().size() + " record(s); replaced current collection.");
                        list();
                    } else out.println("Loaded " + model.listRecords().size() + " record(s); replaced current collection.");
                    break;
                default: throw new IllegalArgumentException("Unknown command. Type help.");
            }
        } catch (IllegalArgumentException | NoSuchElementException | IOException | DateTimeException e) {
            if (view != null) view.error(e.getMessage());
            else out.println("Error: " + e.getMessage());
        }
        out.flush(); return true;
    }
    private static Path file(String name) {
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pim")) throw new IllegalArgumentException("File must end in .pim");
        return Paths.get(name);
    }
    private static LocalDateTime time(String value) {
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}")) {
            throw new IllegalArgumentException("Invalid date/time '" + value + "': expected yyyy-MM-dd HH:mm");
        }
        try {
            return LocalDateTime.parse(value, TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date/time '" + value + "': date does not exist");
        }
    }
    private void create(List<String> a) {
        if (a.size() < 2) throw new IllegalArgumentException("Usage: create TYPE fields...");
        PIR r;
        switch (a.get(1).toLowerCase(Locale.ROOT)) {
            case "note": count(a,3); r=model.createNote(a.get(2)); break;
            case "task": count(a,4); r=model.createTask(a.get(2),time(a.get(3))); break;
            case "event": count(a,5); r=model.createEvent(a.get(2),time(a.get(3)),time(a.get(4))); break;
            case "contact": count(a,5); r=model.createContact(a.get(2),a.get(3),a.get(4)); break;
            default: throw new IllegalArgumentException("Unknown record type");
        }
        unsaved = true;
        if (view != null) {
            view.message("Created " + r.getType().toString().toLowerCase(Locale.ROOT) + " #" + numberOf(r) + ".");
            view.hint("ID: " + r.getId());
            view.hint("View details: print " + numberOf(r));
        } else out.println("Created " + r.getId() + "  [#" + numberOf(r) + "]");
    }
    /** Partial update preserves other fields, stable ID and type. */
    private void update(List<String> a) {
        if (a.size()<4 || a.size()%2!=0) throw new IllegalArgumentException("Usage: update ID field value [field value ...]");
        String id = resolveId(a.get(1));
        PIR r=model.getRecord(id);
        Map<String,String> f=new LinkedHashMap<String,String>();
        for(int i=2;i<a.size();i+=2) {
            String key=a.get(i).toLowerCase(Locale.ROOT);
            if(f.put(key,a.get(i+1))!=null) throw new IllegalArgumentException("Duplicate field: "+key);
        }
        switch(r.getType()) {
            case NOTE: {
                allowed(f,"text"); Note n=(Note)r;
                model.updateNote(r.getId(),f.getOrDefault("text",n.getText())); break;
            }
            case TASK: {
                allowed(f,"description","deadline"); Task t=(Task)r;
                model.updateTask(r.getId(),f.getOrDefault("description",t.getDescription()),f.containsKey("deadline")?time(f.get("deadline")):t.getDeadline()); break;
            }
            case EVENT: {
                allowed(f,"description","start","alarm"); Event e=(Event)r;
                model.updateEvent(r.getId(),f.getOrDefault("description",e.getDescription()),f.containsKey("start")?time(f.get("start")):e.getStartTime(),f.containsKey("alarm")?time(f.get("alarm")):e.getAlarmTime()); break;
            }
            case CONTACT: {
                allowed(f,"name","address","mobile"); Contact c=(Contact)r;
                model.updateContact(r.getId(),f.getOrDefault("name",c.getName()),f.getOrDefault("address",c.getAddress()),f.getOrDefault("mobile",c.getMobileNumber())); break;
            }
            default: throw new IllegalArgumentException("Unsupported type");
        }
        unsaved = true;
        if (view != null) {
            view.message("Updated record #" + numberOf(r) + ".");
            showOne(model.getRecord(r.getId()));
        } else out.println("Updated " + r.getId());
    }
    private static void allowed(Map<String,String> fields,String... names) {
        for(String key:fields.keySet()) if(!Arrays.asList(names).contains(key)) throw new IllegalArgumentException("Invalid field for this type: "+key);
    }
    private static void count(List<String> a,int n) {
        if(a.size()!=n) throw new IllegalArgumentException("Wrong number of arguments. Type help.");
    }
    private void show(List<PIR> records) {
        if (view != null) view.heading(records.size() + " record(s)");
        else out.println(records.size()+" record(s)");
        for(PIR r:records) {
            if (view == null) out.println("Record #" + numberOf(r));
            showOne(r);
        }
    }
    private void showOne(PIR record) {
        if (view != null) view.record(record, numberOf(record));
        else out.println(record.toDetailString());
    }
    private void list() {
        List<PIR> records = model.listRecords();
        if (view != null) {
            view.heading("YOUR RECORDS  /  " + records.size() + " record(s)");
            for (PIR record : records) view.summary(record, numberOf(record));
            view.hint(records.isEmpty() ? "Start with: create note \"Buy milk\""
                    : "Use a displayed number: print " + numberOf(records.get(0)) + "  |  help for update examples");
        } else {
            out.println(records.size() + " record(s)");
            for (PIR record : records) {
                String[] detail = record.toDetailString().split("\n", -1);
                out.println("#" + numberOf(record) + "  " + record.getType()
                        + "  " + (detail.length > 1 ? detail[1] : ""));
            }
            out.println("Use print NUMBER for full details. Numbers stay with their records during this session.");
        }
    }
    /** Session references never shift after search/deletion or get reused for another ID. */
    private int numberOf(PIR record) {
        Integer number = numbers.get(record.getId());
        if (number == null) {
            number = nextNumber++;
            numbers.put(record.getId(), number);
            idsByNumber.put(number, record.getId());
        }
        return number;
    }
    /**
     * Accept a full ID or any unique prefix and return the canonical full ID.
     * An ambiguous prefix is reported with the number of matching records.
     */
    private String resolveId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID must be nonblank");
        }
        if (id.startsWith("id:")) return resolveCanonicalId(id.substring(3));
        if (id.matches("#?[0-9]+")) {
            String digits = id.startsWith("#") ? id.substring(1) : id;
            int number;
            try { number = Integer.parseInt(digits); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Record number is too large. Type list to see available numbers."); }
            String alias = idsByNumber.get(number);
            String exact = null;
            if (!id.startsWith("#")) {
                for (PIR record : model.listRecords()) if (record.getId().equals(id)) exact = id;
            }
            if (alias != null && exact != null && !alias.equals(exact)) {
                throw new IllegalArgumentException("Ambiguous reference '" + id + "': use #" + number + " for the displayed number or id:" + id + " for the stored ID.");
            }
            if (exact != null) return exact;
            if (alias != null) {
                for (PIR record : model.listRecords()) if (record.getId().equals(alias)) return alias;
            }
            throw new IllegalArgumentException("No current record #" + number + ". Type list to see available numbers.");
        }
        if (id.equalsIgnoreCase("TASK_ID") || id.equalsIgnoreCase("CONTACT_ID") || id.equalsIgnoreCase("ID")) {
            throw new IllegalArgumentException(id + " is an example placeholder. Type list, then use a displayed number, for example: print 1");
        }
        return resolveCanonicalId(id);
    }
    private String resolveCanonicalId(String id) {
        if (id.trim().isEmpty()) throw new IllegalArgumentException("ID must be nonblank");
        String needle = id.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<String>();
        for (PIR record : model.listRecords()) {
            String candidate = record.getId().toLowerCase(Locale.ROOT);
            if (candidate.equals(needle)) return record.getId();
            if (candidate.startsWith(needle)) matches.add(record.getId());
        }
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) throw new RecordNotFoundException(id);
        throw new IllegalArgumentException("Ambiguous ID prefix '" + id + "': "
                + matches.size() + " records match; type more characters");
    }
    private void help() {
        List<String> lines = new ArrayList<String>(Arrays.asList(
                "create note TEXT", "create task DESCRIPTION DEADLINE",
                "create event DESCRIPTION START ALARM", "create contact NAME ADDRESS MOBILE",
                "list                    Short record numbers and summaries",
                "print NUMBER|all        Full details, including the stored ID",
                "update NUMBER FIELD VALUE [FIELD VALUE ...]", "delete NUMBER",
                "search CRITERIA", "save FILE.pim", "load FILE.pim", "clear", "help", "quit",
                "", "Quote values containing spaces. Time: yyyy-MM-dd HH:mm.",
                "Fields: text, description, deadline, start, alarm, name, address, mobile.",
                "Use a number from list (1 or #1). Numbers never move to another record during this session.",
                "Numbers are not saved. After restarting, use list to see the current numbers.",
                "Full IDs and unique non-numeric prefixes still work; id:VALUE forces stored-ID lookup.",
                "Save overwrites its file. Load replaces records only on success. Quit/EOF does not save."));
        if (!model.listRecords().isEmpty()) {
            lines.add(""); lines.add("EXAMPLES FOR YOUR CURRENT RECORDS");
            Set<RecordType> shown = new HashSet<RecordType>();
            for (PIR record : model.listRecords()) {
                if (!shown.add(record.getType())) continue;
                String change;
                switch (record.getType()) {
                    case TASK: change = "deadline \"2026-11-20 18:00\""; break;
                    case EVENT: change = "description \"Updated meeting\""; break;
                    case CONTACT: change = "name \"Alice\""; break;
                    default: change = "text \"Buy tea\"";
                }
                lines.add("update " + numberOf(record) + " " + change);
            }
        }
        lines.add("search (type = task || type = event) && description contains \"report\"");
        if (view != null) view.help(lines);
        else for (String line : lines) out.println(line);
    }
    /** Double quotes group arguments; doubled quotes inside a quoted value mean a literal quote. Backslashes stay literal. */
    private static List<String> tokens(String line) {
        List<String> result=new ArrayList<String>(); StringBuilder word=new StringBuilder();
        boolean quoted=false, started=false;
        for(int i=0;i<line.length();i++) {
            char c=line.charAt(i);
            if(c=='"') {
                started=true;
                if(quoted && i+1<line.length() && line.charAt(i+1)=='"') { word.append('"'); i++; }
                else quoted=!quoted;
            } else if(Character.isWhitespace(c) && !quoted) {
                if(started) {result.add(word.toString());word.setLength(0);started=false;}
            } else {word.append(c);started=true;}
        }
        if(quoted) throw new IllegalArgumentException("Unclosed quote");
        if(started) result.add(word.toString());
        return result;
    }
}
