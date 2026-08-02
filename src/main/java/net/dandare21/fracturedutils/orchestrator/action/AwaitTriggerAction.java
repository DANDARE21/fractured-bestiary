package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class AwaitTriggerAction implements OrchestratorAction {
    private String type = "await_trigger";
    private String trigger_id;
    private transient boolean triggered = false;

    public AwaitTriggerAction() {
        this.trigger_id = "";
    }

    public AwaitTriggerAction(String trigger_id) {
        this.trigger_id = trigger_id != null ? trigger_id : "";
    }

    public String getTriggerId() {
        return trigger_id;
    }

    public void setTriggerId(String trigger_id) {
        this.trigger_id = trigger_id != null ? trigger_id : "";
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void trigger() {
        this.triggered = true;
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (triggered) {
            return ActionResult.SUCCESS;
        }
        return ActionResult.BLOCK;
    }

    @Override
    public String getType() {
        return "await_trigger";
    }

    @Override
    public OrchestratorAction copy() {
        return new AwaitTriggerAction(this.trigger_id);
    }
}
