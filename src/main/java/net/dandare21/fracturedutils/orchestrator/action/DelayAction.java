package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class DelayAction implements OrchestratorAction {
    private String type = "delay";
    private int ticks;
    private transient int remainingTicks = -1;

    public DelayAction() {
        this.ticks = 0;
    }

    public DelayAction(int ticks) {
        this.ticks = ticks;
    }

    public int getTicks() {
        return ticks;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (ticks <= 0) {
            return ActionResult.SUCCESS;
        }

        if (remainingTicks < 0) {
            remainingTicks = ticks;
        }

        remainingTicks--;

        if (remainingTicks <= 0) {
            remainingTicks = -1;
            return ActionResult.SUCCESS;
        }

        return ActionResult.BLOCK;
    }

    @Override
    public String getType() {
        return "delay";
    }

    @Override
    public OrchestratorAction copy() {
        return new DelayAction(this.ticks);
    }
}
