package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.checkpoint.CheckpointManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class CheckpointAction implements OrchestratorAction {
    private String type = "checkpoint";
    private double x = 0.0;
    private double y = 64.0;
    private double z = 0.0;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    private String label = "";
    private String targetSelector = "@a";

    public CheckpointAction() {
        this.type = "checkpoint";
    }

    public CheckpointAction(double x, double y, double z, float yaw, float pitch, String label, String targetSelector) {
        this.type = "checkpoint";
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.label = label != null ? label : "";
        this.targetSelector = (targetSelector != null && !targetSelector.isBlank()) ? targetSelector : "@a";
    }

    public CheckpointAction(double x, double y, double z, float yaw, float pitch, String label) {
        this(x, y, z, yaw, pitch, label, "@a");
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public String getLabel() { return label != null ? label : ""; }
    public void setLabel(String label) { this.label = label != null ? label : ""; }

    public String getTargetSelector() { return targetSelector != null && !targetSelector.isBlank() ? targetSelector : "@a"; }
    public void setTargetSelector(String targetSelector) { this.targetSelector = (targetSelector != null && !targetSelector.isBlank()) ? targetSelector : "@a"; }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        CheckpointManager.getInstance().setActiveCheckpoint(instance, instance.getCurrentIndex(), x, y, z, yaw, pitch, label, getTargetSelector());
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "checkpoint";
    }

    @Override
    public OrchestratorAction copy() {
        return new CheckpointAction(x, y, z, yaw, pitch, label, getTargetSelector());
    }
}
