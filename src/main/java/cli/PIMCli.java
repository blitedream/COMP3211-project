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
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public PIMCli(PrintWriter out) { this.out = out; }
    public static void main(String[] args) throws IOException {
        PIMCli cli = new PIMCli(new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true));
        cli.run(new BufferedReader(new InputStreamReader(System.in, "UTF-8")));
    }
    public void run(BufferedReader input) throws IOException {
        out.println("PIM - type help for commands. Save before quitting.");
        String line;
        while (true) {
            out.print("pim> "); out.flush();
            line = input.readLine();
            if (line == null || !execute(line)) break;
        }
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
                case "quit": case "exit": count(a, 1); out.println("Goodbye."); return false;
                case "create": create(a); break;
                case "update": update(a); break;
                case "print":
                    count(a, 2);
                    if (a.get(1).equalsIgnoreCase("all")) show(model.listRecords());
                    else out.println(model.getRecord(resolveId(a.get(1))).toDetailString());
                    break;
                case "delete": {
                    count(a, 2);
                    String id = resolveId(a.get(1));
                    model.deleteRecord(id);
                    out.println("Deleted " + id);
                    break;
                }
                case "save": count(a, 2); PIMFileCodec.save(model, file(a.get(1))); out.println("Saved."); break;
                case "load": count(a, 2); PIMModel loaded = PIMFileCodec.load(file(a.get(1))); model = loaded; out.println("Loaded " + model.listRecords().size() + " record(s); replaced current collection."); break;
                default: throw new IllegalArgumentException("Unknown command. Type help.");
            }
        } catch (IllegalArgumentException | NoSuchElementException | IOException | DateTimeException e) {
            out.println("Error: " + e.getMessage());
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
        out.println("Created " + r.getId());
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
        out.println("Updated " + r.getId());
    }
    private static void allowed(Map<String,String> fields,String... names) {
        for(String key:fields.keySet()) if(!Arrays.asList(names).contains(key)) throw new IllegalArgumentException("Invalid field for this type: "+key);
    }
    private static void count(List<String> a,int n) {
        if(a.size()!=n) throw new IllegalArgumentException("Wrong number of arguments. Type help.");
    }
    private void show(List<PIR> records) {
        out.println(records.size()+" record(s)");
        for(PIR r:records) out.println(r.toDetailString());
    }
    /**
     * Accept a full ID or any unique prefix and return the canonical full ID.
     * An ambiguous prefix is reported with the number of matching records.
     */
    private String resolveId(String id) {
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
        out.println("create note TEXT\ncreate task DESCRIPTION DEADLINE\ncreate event DESCRIPTION START ALARM\ncreate contact NAME ADDRESS MOBILE\nupdate ID FIELD VALUE [FIELD VALUE ...]\nsearch CRITERIA\nprint ID|all\ndelete ID\nsave FILE.pim\nload FILE.pim\nhelp\nquit");
        out.println("Quote values containing spaces. Time: yyyy-MM-dd HH:mm. Fields: text, description, deadline, start, alarm, name, address, mobile.");
        out.println("ID may be the full 36-character ID or any unique prefix (8 characters usually suffice).");
        out.println("Search example: (type = task || type = event) && description contains \"report\"");
        out.println("Save overwrites its file. Load replaces all records only on success. Quit/EOF does not save automatically.");
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
