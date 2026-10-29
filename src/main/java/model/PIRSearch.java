package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Evaluates a search criteria string against PIR records (US7). */
public final class PIRSearch {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private PIRSearch() {
    }

    /**
     * Return matching records in input order.
     * Accepts one comparison, or comparisons combined with {@code !}, {@code &&},
     * {@code ||}, and parentheses. {@code !} binds tighter than {@code &&},
     * which binds tighter than {@code ||}.
     */
    public static List<PIR> search(List<PIR> records, String criteria) {
        if (records == null) {
            throw new InvalidSearchException("records is required");
        }
        Criterion criterion = parse(criteria);
        List<PIR> matches = new ArrayList<PIR>();
        for (PIR record : records) {
            if (record == null) {
                throw new InvalidSearchException("record is required");
            }
            if (criterion.matches(record)) {
                matches.add(record);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    private static Criterion parse(String criteria) {
        if (criteria == null || criteria.trim().isEmpty()) {
            throw new InvalidSearchException("criteria is required");
        }
        Parser parser = new Parser(tokenize(criteria));
        Criterion criterion = parser.parseOr();
        parser.end();
        return criterion;
    }

    private static boolean isTimeField(String field) {
        return field.equalsIgnoreCase("deadline")
                || field.equalsIgnoreCase("start")
                || field.equalsIgnoreCase("alarm");
    }

    private static boolean isTimeOperator(String operator) {
        return "<".equals(operator) || ">".equals(operator) || "=".equals(operator);
    }

    private static LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new InvalidSearchException("Invalid date/time: " + text);
        }
    }

    private static boolean isTextField(String field) {
        return field.equalsIgnoreCase("text")
                || field.equalsIgnoreCase("description")
                || field.equalsIgnoreCase("name")
                || field.equalsIgnoreCase("address")
                || field.equalsIgnoreCase("mobile");
    }

    private static List<Token> tokenize(String criteria) {
        List<Token> tokens = new ArrayList<Token>();
        int index = 0;
        while (index < criteria.length()) {
            char current = criteria.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '"') {
                int start = index + 1;
                int end = criteria.indexOf('"', start);
                if (end < 0) {
                    throw new InvalidSearchException("Unclosed quote");
                }
                if (end == start) {
                    throw new InvalidSearchException("Quoted value is empty");
                }
                tokens.add(new Token(criteria.substring(start, end), true));
                index = end + 1;
                continue;
            }
            if (current == '!' || current == '(' || current == ')') {
                tokens.add(new Token(String.valueOf(current), false));
                index++;
                continue;
            }
            int start = index;
            while (index < criteria.length()
                    && !Character.isWhitespace(criteria.charAt(index))
                    && criteria.charAt(index) != '!'
                    && criteria.charAt(index) != '('
                    && criteria.charAt(index) != ')') {
                if (criteria.charAt(index) == '"') {
                    throw new InvalidSearchException("Unexpected quote");
                }
                index++;
            }
            tokens.add(new Token(criteria.substring(start, index), false));
        }
        return tokens;
    }

    private static RecordType typeNamed(String name) {
        for (RecordType type : RecordType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new InvalidSearchException("Unknown type: " + name);
    }

    private static String textOf(PIR record, String field) {
        if (field.equalsIgnoreCase("text") && record instanceof Note) {
            return ((Note) record).getText();
        }
        if (field.equalsIgnoreCase("description") && record instanceof Task) {
            return ((Task) record).getDescription();
        }
        if (field.equalsIgnoreCase("description") && record instanceof Event) {
            return ((Event) record).getDescription();
        }
        if (field.equalsIgnoreCase("name") && record instanceof Contact) {
            return ((Contact) record).getName();
        }
        if (field.equalsIgnoreCase("address") && record instanceof Contact) {
            return ((Contact) record).getAddress();
        }
        if (field.equalsIgnoreCase("mobile") && record instanceof Contact) {
            return ((Contact) record).getMobileNumber();
        }
        return null;
    }

    private static LocalDateTime timeOf(PIR record, String field) {
        if (field.equalsIgnoreCase("deadline") && record instanceof Task) {
            return ((Task) record).getDeadline();
        }
        if (field.equalsIgnoreCase("start") && record instanceof Event) {
            return ((Event) record).getStartTime();
        }
        if (field.equalsIgnoreCase("alarm") && record instanceof Event) {
            return ((Event) record).getAlarmTime();
        }
        return null;
    }

    private interface Criterion {
        boolean matches(PIR record);
    }

    private static final class TypeCriterion implements Criterion {
        private final RecordType type;

        private TypeCriterion(RecordType type) {
            this.type = type;
        }

        @Override
        public boolean matches(PIR record) {
            return record.getType() == type;
        }
    }

    private static final class TextCriterion implements Criterion {
        private final String field;
        private final String needle;

        private TextCriterion(String field, String needle) {
            this.field = field;
            this.needle = needle;
        }

        @Override
        public boolean matches(PIR record) {
            String value = textOf(record, field);
            return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
        }
    }

    private static final class TimeCriterion implements Criterion {
        private final String field;
        private final String operator;
        private final LocalDateTime instant;

        private TimeCriterion(String field, String operator, LocalDateTime instant) {
            this.field = field;
            this.operator = operator;
            this.instant = instant;
        }

        @Override
        public boolean matches(PIR record) {
            LocalDateTime value = timeOf(record, field);
            if (value == null) {
                return false;
            }
            int compared = value.compareTo(instant);
            if ("<".equals(operator)) {
                return compared < 0;
            }
            if (">".equals(operator)) {
                return compared > 0;
            }
            return compared == 0;
        }
    }

    private static final class NotCriterion implements Criterion {
        private final Criterion inner;

        private NotCriterion(Criterion inner) {
            this.inner = inner;
        }

        @Override
        public boolean matches(PIR record) {
            return !inner.matches(record);
        }
    }

    private static final class AndCriterion implements Criterion {
        private final Criterion left;
        private final Criterion right;

        private AndCriterion(Criterion left, Criterion right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(PIR record) {
            return left.matches(record) && right.matches(record);
        }
    }

    private static final class OrCriterion implements Criterion {
        private final Criterion left;
        private final Criterion right;

        private OrCriterion(Criterion left, Criterion right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(PIR record) {
            return left.matches(record) || right.matches(record);
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private int index;

        private Parser(List<Token> tokens) {
            this.tokens = tokens;
        }

        private Criterion parseOr() {
            Criterion left = parseAnd();
            while (consume("||")) {
                left = new OrCriterion(left, parseAnd());
            }
            return left;
        }

        private Criterion parseAnd() {
            Criterion left = parseNot();
            while (consume("&&")) {
                left = new AndCriterion(left, parseNot());
            }
            return left;
        }

        private Criterion parseNot() {
            if (consume("!")) {
                return new NotCriterion(parseNot());
            }
            return parsePrimary();
        }

        private Criterion parsePrimary() {
            if (consume("(")) {
                Criterion inner = parseOr();
                expect(")");
                return inner;
            }
            return parseComparison();
        }

        private Criterion parseComparison() {
            if (index + 2 >= tokens.size()) {
                throw new InvalidSearchException("Operator is missing an operand");
            }
            Token fieldToken = tokens.get(index);
            Token operatorToken = tokens.get(index + 1);
            Token value = tokens.get(index + 2);
            if (fieldToken.quoted || operatorToken.quoted) {
                throw new InvalidSearchException("Malformed criteria");
            }
            index += 3;
            String field = fieldToken.text;
            String operator = operatorToken.text;
            if (field.equalsIgnoreCase("type")) {
                if (!operator.equals("=") || value.quoted) {
                    throw new InvalidSearchException("Expected: type = note|task|event|contact");
                }
                return new TypeCriterion(typeNamed(value.text));
            }
            if (isTextField(field)) {
                if (!operator.equalsIgnoreCase("contains") || !value.quoted) {
                    throw new InvalidSearchException("Expected: " + field + " contains \"...\"");
                }
                return new TextCriterion(field, value.text.toLowerCase(Locale.ROOT));
            }
            if (isTimeField(field)) {
                if (!isTimeOperator(operator) || !value.quoted) {
                    throw new InvalidSearchException(
                            "Expected: " + field + " <, >, or = \"yyyy-MM-dd HH:mm\"");
                }
                return new TimeCriterion(field, operator, parseTime(value.text));
            }
            throw new InvalidSearchException("Unknown field: " + field);
        }

        private boolean consume(String text) {
            if (index < tokens.size() && !tokens.get(index).quoted
                    && tokens.get(index).text.equals(text)) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(String text) {
            if (!consume(text)) {
                throw new InvalidSearchException("Expected " + text);
            }
        }

        private void end() {
            if (index < tokens.size()) {
                throw new InvalidSearchException("Unexpected token: " + tokens.get(index).text);
            }
        }
    }

    private static final class Token {
        private final String text;
        private final boolean quoted;

        private Token(String text, boolean quoted) {
            this.text = text;
            this.quoted = quoted;
        }
    }
}
