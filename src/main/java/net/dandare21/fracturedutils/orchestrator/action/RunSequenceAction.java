package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.orchestrator.SequenceState;
import net.minecraft.server.MinecraftServer;

public class RunSequenceAction implements OrchestratorAction {
    private String type = "run_sequence";
    private String file;
    private int startIndex = 0;
    private transient SequenceInstance spawnedChild = null;

    public RunSequenceAction() {
        this.file = "";
        this.startIndex = 0;
    }

    public RunSequenceAction(String file) {
        this(file, 0);
    }

    public RunSequenceAction(String file, int startIndex) {
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
        if (file == null || file.isEmpty()) {
            return ActionResult.SUCCESS;
        }

        if (spawnedChild == null) {
            SequenceInstance child = OrchestratorManager.getInstance().createSequenceInstance(file, instance.getTargetPlayerName(), instance, startIndex);
            if (child == null) {
                return ActionResult.SUCCESS;
            }
            spawnedChild = child;
            instance.getActiveChildren().add(spawnedChild);
            return ActionResult.BLOCK;
        }

        if (spawnedChild.getState() == SequenceState.FINISHED || !instance.getActiveChildren().contains(spawnedChild)) {
            spawnedChild = null;
            return ActionResult.SUCCESS;
        }

        return ActionResult.BLOCK;
    }

    @Override
    public String getType() {
        return "run_sequence";
    }

    @Override
    public OrchestratorAction copy() {
        return new RunSequenceAction(this.file, this.startIndex);
    }
}
