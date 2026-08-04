package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class ForkSequenceAction implements OrchestratorAction {
    private String type = "fork_sequence";
    private String file;
    private int startIndex = 0;

    public ForkSequenceAction() {
        this.file = "";
        this.startIndex = 0;
    }

    public ForkSequenceAction(String file) {
        this(file, 0);
    }

    public ForkSequenceAction(String file, int startIndex) {
        this.file = file != null ? file : "";
        this.startIndex = Math.max(0, startIndex);
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file != null ? file : "";
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = Math.max(0, startIndex);
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (file != null && !file.isEmpty()) {
            SequenceInstance child = OrchestratorManager.getInstance().createSequenceInstance(file, instance.getTargetPlayerName(), instance, startIndex);
            if (child != null) {
                instance.getActiveChildren().add(child);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "fork_sequence";
    }

    @Override
    public OrchestratorAction copy() {
        return new ForkSequenceAction(this.file, this.startIndex);
    }
}
