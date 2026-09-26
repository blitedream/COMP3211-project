package model;

import java.time.LocalDateTime;

/** To-do PIR with a description and deadline (US3). */
public final class Task extends PIR {
    private final String description;
    private final LocalDateTime deadline;

    public Task(String id, String description, LocalDateTime deadline) {
        super(id);
        this.description = requireText(description, "description");
        this.deadline = requireValue(deadline, "deadline");
    }

    @Override
    public RecordType getType() {
        return RecordType.TASK;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
}
