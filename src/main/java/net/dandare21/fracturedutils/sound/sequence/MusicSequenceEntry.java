package net.dandare21.fracturedutils.sound.sequence;

public class MusicSequenceEntry {
    private long timestampMs;
    private String actionType;
    private String command;
    private String description;

    public MusicSequenceEntry() {
        this.timestampMs = 0L;
        this.actionType = "COMMAND";
        this.command = "";
        this.description = "";
    }

    public MusicSequenceEntry(long timestampMs, String actionType, String command, String description) {
        this.timestampMs = Math.max(0L, timestampMs);
        this.actionType = actionType != null ? actionType : "COMMAND";
        this.command = command != null ? command : "";
        this.description = description != null ? description : "";
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(long timestampMs) {
        this.timestampMs = Math.max(0L, timestampMs);
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType != null ? actionType : "COMMAND";
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command != null ? command : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public MusicSequenceEntry copy() {
        return new MusicSequenceEntry(this.timestampMs, this.actionType, this.command, this.description);
    }
}
