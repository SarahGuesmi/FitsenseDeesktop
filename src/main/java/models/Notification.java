package models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String type;       // e.g. "NEW_USER"
    private String message;
    private String targetEmail;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String type, String message, String targetEmail, LocalDateTime createdAt) {
        this.type = type;
        this.message = message;
        this.targetEmail = targetEmail;
        this.read = false;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTargetEmail() { return targetEmail; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
