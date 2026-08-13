package net.dandare21.fracturedutils.dialog;

import java.util.ArrayList;
import java.util.List;

public class DialogSequence {
    private String name;
    private List<DialogLine> lines;

    public DialogSequence() {
        this.name = "new_dialog.json";
        this.lines = new ArrayList<>();
    }

    public DialogSequence(String name, List<DialogLine> lines) {
        this.name = name != null ? name : "new_dialog.json";
        this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public List<DialogLine> getLines() {
        return lines;
    }

    public void setLines(List<DialogLine> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }
}
