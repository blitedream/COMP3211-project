package model;

import java.time.LocalDateTime;

/** Schedule PIR with a start and alarm time (US4). */
public final class Event extends PIR {
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime alarmTime;

    public Event(String id, String description, LocalDateTime startTime,
                 LocalDateTime alarmTime) {
        super(id);
        this.description = requireText(description, "description");
        this.startTime = requireValue(startTime, "startTime");
        this.alarmTime = requireValue(alarmTime, "alarmTime");
    }

    @Override
    public RecordType getType() {
        return RecordType.EVENT;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }
}
