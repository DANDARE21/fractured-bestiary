package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class ForkSequenceAction implements OrchestratorAction {
    private String type = "fork_sequence";
    private String file;

    public ForkSequenceAction() {
        this.file = "";
    }

    public ForkSequenceAction(String file) {
        this.file = file != null ? file : "";
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file != null ? file : "";
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (file != null && !file.isEmpty()) {
            SequenceInstance child = OrchestratorManager.getInstance().createSequenceInstance(file, instance.getTargetPlayerName(), instance);
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
        return new ForkSequenceAction(this.file);
    }
}
