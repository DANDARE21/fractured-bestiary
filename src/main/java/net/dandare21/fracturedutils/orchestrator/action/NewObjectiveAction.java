package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.objective.ObjectiveManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class NewObjectiveAction implements OrchestratorAction {
    private String type = "new_objective";
    private String name = "";
    private String description = "";
    private boolean showActiveWait = false;

    public NewObjectiveAction() {
        this.type = "new_objective";
        this.name = "";
        this.description = "";
        this.showActiveWait = false;
    }

    public NewObjectiveAction(String name, String description, boolean showActiveWait) {
        this.type = "new_objective";
        this.name = name != null ? name : "";
        this.description = description != null ? description : "";
        this.showActiveWait = showActiveWait;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public boolean isShowActiveWait() {
        return showActiveWait;
    }

    public void setShowActiveWait(boolean showActiveWait) {
        this.showActiveWait = showActiveWait;
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        ObjectiveManager.getInstance().setObjective(instance, server, getName(), getDescription(), isShowActiveWait());
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "new_objective";
    }

    @Override
    public OrchestratorAction copy() {
        return new NewObjectiveAction(name, description, showActiveWait);
    }
}
