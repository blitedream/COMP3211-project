package model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Saves a PIMModel to a .pim file, or loads one back from it (US10, US11). */
public final class PIMFileCodec {

    /** Same time format as B's PIRSearch. */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private PIMFileCodec() {
    }

    /** US10: write all PIRs to a file. An existing file is overwritten. */
    public static void save(PIMModel model, Path file) throws IOException {
        StringBuilder content = new StringBuilder();
        for (PIR record : model.listRecords()) {
            writeBlock(content, record);
            content.append('\n'); // blank line between record blocks
        }
        Files.write(file, content.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBlock(StringBuilder out, PIR record) {
        out.append("type=").append(record.getType().name()).append('\n');
        out.append("id=").append(record.getId()).append('\n');
        if (record instanceof Note) {
            Note note = (Note) record;
            out.append("text=").append(encode(note.getText())).append('\n');
        } else if (record instanceof Task) {
            Task task = (Task) record;
            out.append("description=").append(encode(task.getDescription())).append('\n');
            out.append("deadline=").append(format(task.getDeadline())).append('\n');
        } else if (record instanceof Event) {
            Event event = (Event) record;
            out.append("description=").append(encode(event.getDescription())).append('\n');
            out.append("start=").append(format(event.getStartTime())).append('\n');
            out.append("alarm=").append(format(event.getAlarmTime())).append('\n');
        } else if (record instanceof Contact) {
            Contact contact = (Contact) record;
            out.append("name=").append(encode(contact.getName())).append('\n');
            out.append("address=").append(encode(contact.getAddress())).append('\n');
            out.append("mobile=").append(encode(contact.getMobileNumber())).append('\n');
        } else {
            throw new IllegalArgumentException("Unsupported PIR type");
        }
    }

    /** US11: load from a file. An empty file is an empty model; a malformed file throws a clear error. */
    public static PIMModel load(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<PIR> records = new ArrayList<PIR>();
        List<String> block = new ArrayList<String>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (!block.isEmpty()) {
                    records.add(parseBlock(block));
                    block = new ArrayList<String>();
                }
            } else {
                block.add(line);
            }
        }
        if (!block.isEmpty()) {
            records.add(parseBlock(block));
        }
        // Duplicate IDs are rejected by the PIMModel constructor (implemented by A)
        return new PIMModel(records);
    }

    private static PIR parseBlock(List<String> block) {
        String type = null, id = null, text = null, description = null;
        String name = null, address = null, mobile = null;
        LocalDateTime deadline = null, start = null, alarm = null;

        for (String line : block) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new InvalidRecordException("Malformed line: " + line);
            }
            String key = line.substring(0, eq);
            String value = line.substring(eq + 1);
            if (key.equals("type")) {
                type = value;
            } else if (key.equals("id")) {
                id = value;
            } else if (key.equals("text")) {
                text = decode(value);
            } else if (key.equals("description")) {
                description = decode(value);
            } else if (key.equals("deadline")) {
                deadline = parseTime(value);
            } else if (key.equals("start")) {
                start = parseTime(value);
            } else if (key.equals("alarm")) {
                alarm = parseTime(value);
            } else if (key.equals("name")) {
                name = decode(value);
            } else if (key.equals("address")) {
                address = decode(value);
            } else if (key.equals("mobile")) {
                mobile = decode(value);
            } else {
                throw new InvalidRecordException("Unknown field: " + key);
            }
        }
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidRecordException("id is missing");
        }
        if (type == null) {
            throw new InvalidRecordException("type is missing");
        }
        if (type.equals("NOTE")) {
            return new Note(id, requireText(text, "text"));
        }
        if (type.equals("TASK")) {
            return new Task(id, requireText(description, "description"),
                    requireTime(deadline, "deadline"));
        }
        if (type.equals("EVENT")) {
            return new Event(id, requireText(description, "description"),
                    requireTime(start, "start"), requireTime(alarm, "alarm"));
        }
        if (type.equals("CONTACT")) {
            return new Contact(id, requireText(name, "name"),
                    requireText(address, "address"), requireText(mobile, "mobile"));
        }
        throw new InvalidRecordException("Unknown type: " + type);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRecordException(field + " is missing");
        }
        return value;
    }

    private static LocalDateTime requireTime(LocalDateTime value, String field) {
        if (value == null) {
            throw new InvalidRecordException(field + " is missing");
        }
        return value;
    }

    private static String format(LocalDateTime time) {
        return TIME_FORMAT.format(time);
    }

    private static LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new InvalidRecordException("Invalid date/time: " + value);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getDecoder().decode(value),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRecordException("Invalid base64 value");
        }
    }
}
