package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.objective.ObjectiveManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class EndObjectiveAction implements OrchestratorAction {
    private String type = "end_objective";

    public EndObjectiveAction() {
        this.type = "end_objective";
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        ObjectiveManager.getInstance().clearObjective(instance, server);
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "end_objective";
    }

    @Override
    public OrchestratorAction copy() {
        return new EndObjectiveAction();
    }
}
