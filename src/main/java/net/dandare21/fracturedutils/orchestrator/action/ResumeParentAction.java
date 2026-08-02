package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.orchestrator.SequenceState;
import net.minecraft.server.MinecraftServer;

public class ResumeParentAction implements OrchestratorAction {
    private String type = "resume_parent";

    public ResumeParentAction() {
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (instance.getParent() != null) {
            instance.getParent().setState(SequenceState.RUNNING);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "resume_parent";
    }

    @Override
    public OrchestratorAction copy() {
        return new ResumeParentAction();
    }
}
