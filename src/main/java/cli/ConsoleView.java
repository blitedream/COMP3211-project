package cli;

import java.io.PrintWriter;
import java.util.List;
import model.PIR;

/** ANSI terminal presentation only; no model mutations or third-party dependencies. */
final class ConsoleView {
    static final String RESET = "\u001b[0m";
    private static final String BG = "\u001b[48;2;11;18;32m";
    private static final String TEXT = "\u001b[38;2;237;243;250m";
    private static final String DIM = "\u001b[38;2;152;171;196m";
    private static final String ACCENT = "\u001b[38;2;103;223;192m";
    private static final String ERROR = "\u001b[38;2;245;204;117m";
    private final int width;
    private final PrintWriter out;

    ConsoleView(PrintWriter out) {
        this.out = out;
        this.width = Math.max(32, Math.min(100, Integer.getInteger("pim.columns", 100) - 8));
    }

    void welcome() {
        out.print(BG + TEXT + "\u001b[2J\u001b[H");
        border('╭', '╮');
        row("PIM  /  PERSONAL INFORMATION MANAGER", ACCENT);
        row("Notes  ·  Tasks  ·  Events  ·  Contacts", DIM);
        row("", TEXT);
        row("QUICK START", ACCENT);
        row("create note \"Buy milk\"", TEXT);
        row("list          Show record numbers and summaries", TEXT);
        row("print 1       Show every field of record #1", TEXT);
        row("help          Commands and ready-to-use examples", TEXT);
        row("", TEXT);
        row("Use short numbers such as 1 or #1. Save before quitting.", DIM);
        border('╰', '╯');
        out.println();
    }

    void prompt(int count, boolean unsaved) {
        out.println(BG + DIM + "  " + count + " record(s)  ·  "
                + (unsaved ? "Unsaved changes" : "No unsaved changes") + "\u001b[K");
        out.print(BG + ACCENT + "  pim> " + TEXT);
        out.flush();
    }

    void heading(String title) {
        out.println();
        row(title, ACCENT);
    }

    void message(String text) { row(text, ACCENT); }
    void hint(String text) { row(text, DIM); }
    void error(String text) { row("Error: " + text, ERROR); }

    void record(PIR record, int number) {
        border('╭', '╮');
        row("#" + number + "  " + record.getType(), ACCENT);
        String[] detail = record.toDetailString().split("\n", -1);
        for (int i = 1; i < detail.length; i++) row(detail[i], TEXT);
        row("ID: " + record.getId(), DIM);
        border('╰', '╯');
    }

    void summary(PIR record, int number) {
        String[] detail = record.toDetailString().split("\n", -1);
        String first = detail.length > 1 ? detail[1] : "";
        int separator = first.indexOf(": ");
        if (separator >= 0) first = first.substring(separator + 2);
        String label = String.format("#%-5d %-9s ", number, record.getType());
        row(label + first, TEXT);
    }

    void help(List<String> lines) {
        border('╭', '╮');
        row("COMMAND REFERENCE", ACCENT);
        for (String line : lines) row(line, TEXT);
        border('╰', '╯');
    }

    private void border(char left, char right) {
        StringBuilder line = new StringBuilder("  ").append(left);
        for (int i = 0; i < width + 2; i++) line.append('─');
        out.println(BG + DIM + line.append(right) + "\u001b[K");
    }

    /** Wrap full field values without executing terminal controls from stored text. */
    private void row(String value, String color) {
        StringBuilder line = new StringBuilder();
        int columns = 0;
        String text = printable(value);
        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            int cellWidth = width(cp);
            if (columns + cellWidth > width) {
                writeRow(line.toString(), columns, color);
                line.setLength(0); columns = 0;
            }
            line.appendCodePoint(cp); columns += cellWidth;
            offset += Character.charCount(cp);
        }
        writeRow(line.toString(), columns, color);
    }

    private void writeRow(String line, int columns, String color) {
        StringBuilder padded = new StringBuilder(line);
        for (int i = columns; i < width; i++) padded.append(' ');
        out.println(BG + DIM + "  │ " + color + padded + DIM + " │\u001b[K");
    }

    private static String printable(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isISOControl(c)) result.append(String.format("\\u%04x", (int)c));
            else result.append(c);
        }
        return result.toString();
    }

    private static int width(int cp) {
        int type = Character.getType(cp);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK) return 0;
        return (cp >= 0x1100 && (cp <= 0x115f || cp == 0x2329 || cp == 0x232a
                || (cp >= 0x2e80 && cp <= 0xa4cf) || (cp >= 0xac00 && cp <= 0xd7a3)
                || (cp >= 0xf900 && cp <= 0xfaff) || (cp >= 0xfe10 && cp <= 0xfe19)
                || (cp >= 0xfe30 && cp <= 0xfe6f) || (cp >= 0xff00 && cp <= 0xff60)
                || (cp >= 0xffe0 && cp <= 0xffe6) || (cp >= 0x1f300 && cp <= 0x1faff)
                || (cp >= 0x20000 && cp <= 0x3fffd))) ? 2 : 1;
    }
}
