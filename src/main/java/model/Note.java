package model;

/** Plain-text PIR (US2). */
public final class Note extends PIR {
    private final String text;

    public Note(String id, String text) {
        super(id);
        this.text = requireText(text, "text");
    }

    @Override
    public RecordType getType() {
        return RecordType.NOTE;
    }

    public String getText() {
        return text;
    }
}
