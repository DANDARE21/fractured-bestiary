package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.puppet.IPuppetEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class ExecutePuppetAction implements OrchestratorAction {
    private String type = "puppet_action";
    private String actionId;
    private String entityUuid;
    private int durationTicks = 0;

    public ExecutePuppetAction() {
        this.actionId = "";
        this.entityUuid = "";
        this.durationTicks = 0;
    }

    public ExecutePuppetAction(String actionId, String entityUuid, int durationTicks) {
        this.actionId = actionId != null ? actionId : "";
        this.entityUuid = entityUuid != null ? entityUuid : "";
        this.durationTicks = durationTicks;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId != null ? actionId : ""; }

    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid != null ? entityUuid : ""; }

    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int durationTicks) { this.durationTicks = durationTicks; }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (server == null || actionId == null || actionId.isBlank()) {
            return ActionResult.SUCCESS;
        }

        ResourceLocation resLoc = ResourceLocation.tryParse(actionId);
        if (resLoc == null) {
            return ActionResult.SUCCESS;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (entityUuid != null && !entityUuid.isBlank()) {
                try {
                    UUID uuid = UUID.fromString(entityUuid);
                    Entity entity = level.getEntity(uuid);
                    if (entity instanceof IPuppetEntity puppetEntity) {
                        puppetEntity.getPuppetController().executeAction(resLoc, new CompoundTag(), durationTicks, null);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof IPuppetEntity puppetEntity) {
                        puppetEntity.getPuppetController().executeAction(resLoc, new CompoundTag(), durationTicks, null);
                    }
                }
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "puppet_action";
    }

    @Override
    public OrchestratorAction copy() {
        return new ExecutePuppetAction(this.actionId, this.entityUuid, this.durationTicks);
    }
}
