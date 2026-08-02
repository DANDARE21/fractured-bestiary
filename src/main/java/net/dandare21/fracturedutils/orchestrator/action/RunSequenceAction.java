package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.orchestrator.SequenceState;
import net.minecraft.server.MinecraftServer;

public class RunSequenceAction implements OrchestratorAction {
    private String type = "run_sequence";
    private String file;
    private transient SequenceInstance spawnedChild = null;

    public RunSequenceAction() {
        this.file = "";
    }

    public RunSequenceAction(String file) {
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
        if (file == null || file.isEmpty()) {
            return ActionResult.SUCCESS;
        }

        if (spawnedChild == null) {
            SequenceInstance child = OrchestratorManager.getInstance().createSequenceInstance(file, instance.getTargetPlayerName(), instance);
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
        return new RunSequenceAction(this.file);
    }
}
