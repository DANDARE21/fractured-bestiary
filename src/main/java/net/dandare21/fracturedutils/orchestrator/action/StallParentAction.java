package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.orchestrator.SequenceState;
import net.minecraft.server.MinecraftServer;

public class StallParentAction implements OrchestratorAction {
    private String type = "stall_parent";

    public StallParentAction() {
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (instance.getParent() != null) {
            instance.getParent().setState(SequenceState.STALLED);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "stall_parent";
    }

    @Override
    public OrchestratorAction copy() {
        return new StallParentAction();
    }
}
