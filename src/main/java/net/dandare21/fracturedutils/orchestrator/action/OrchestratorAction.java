package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public interface OrchestratorAction {
    ActionResult execute(SequenceInstance instance, MinecraftServer server);
    String getType();
    OrchestratorAction copy();
}
