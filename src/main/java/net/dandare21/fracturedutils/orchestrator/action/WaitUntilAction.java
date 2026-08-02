package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.OrchestratorManager;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.server.MinecraftServer;

public class WaitUntilAction implements OrchestratorAction {
    private String type = "wait_until";
    private String waitType = "delay"; // "delay", "trigger", "operator_action", "downloads"
    private int ticks = 20;
    private String triggerId = "";
    private String label = "";
    private transient int remainingTicks = -1;
    private transient boolean triggered = false;
    private transient boolean hasSeenActive = false;
    private transient int graceTicks = 0;

    public WaitUntilAction() {
        this.type = "wait_until";
        this.waitType = "delay";
        this.ticks = 20;
        this.triggerId = "";
        this.label = "";
    }

    public WaitUntilAction(String waitType, int ticks, String triggerId, String label) {
        this.type = "wait_until";
        this.waitType = waitType != null ? waitType : "delay";
        this.ticks = Math.max(0, ticks);
        this.triggerId = triggerId != null ? triggerId : "";
        this.label = label != null ? label : "";
    }

    public String getWaitType() {
        return waitType != null ? waitType : "delay";
    }

    public void setWaitType(String waitType) {
        this.waitType = waitType != null ? waitType : "delay";
    }

    public int getTicks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    public String getTriggerId() {
        return triggerId != null ? triggerId : "";
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId != null ? triggerId : "";
    }

    public String getLabel() {
        return label != null ? label : "";
    }

    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    public void trigger() {
        this.triggered = true;
    }

    public boolean isTriggered() {
        return triggered;
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        String mode = getWaitType().toLowerCase();

        if (mode.equals("delay")) {
            if (remainingTicks < 0) {
                remainingTicks = ticks;
            }
            if (remainingTicks <= 0) {
                remainingTicks = -1;
                return ActionResult.SUCCESS;
            }
            remainingTicks--;
            return ActionResult.BLOCK;
        } else if (mode.equals("operator_action")) {
            String effectiveTrigger = (triggerId != null && !triggerId.isBlank()) ? triggerId : ("op_action_" + instance.getSequenceName());
            if (triggered) {
                OrchestratorManager.getInstance().unregisterOperatorAction(server, effectiveTrigger);
                triggered = false;
                return ActionResult.SUCCESS;
            }
            OrchestratorManager.getInstance().registerOperatorAction(server, effectiveTrigger, (label != null && !label.isBlank()) ? label : ("Resume " + instance.getSequenceName()));
            return ActionResult.BLOCK;
        } else if (mode.equals("video") || mode.equals("video_end") || mode.equals("cutscene") || mode.equals("cinematic")) {
            boolean activeNow = net.dandare21.fracturedutils.cutscene.ServerCutsceneManager.getInstance().isCutsceneActive();
            if (activeNow) {
                hasSeenActive = true;
                return ActionResult.BLOCK;
            }
            if (hasSeenActive) {
                hasSeenActive = false;
                graceTicks = 0;
                return ActionResult.SUCCESS;
            }
            graceTicks++;
            if (graceTicks < 20) {
                return ActionResult.BLOCK;
            }
            graceTicks = 0;
            return ActionResult.SUCCESS;
        } else if (mode.equals("waiting_room") || mode.equals("waiting_room_end") || mode.equals("waitingroom")) {
            boolean activeNow = net.dandare21.fracturedutils.waitingroom.WaitingRoomManager.getInstance().isActive();
            if (activeNow) {
                hasSeenActive = true;
                return ActionResult.BLOCK;
            }
            if (hasSeenActive) {
                hasSeenActive = false;
                graceTicks = 0;
                return ActionResult.SUCCESS;
            }
            graceTicks++;
            if (graceTicks < 20) {
                return ActionResult.BLOCK;
            }
            graceTicks = 0;
            return ActionResult.SUCCESS;
        } else if (mode.equals("downloads") || mode.equals("downloads_end") || mode.equals("cutscene_downloads") || mode.equals("video_downloads")) {
            boolean allFinished = net.dandare21.fracturedutils.cutscene.ServerCutsceneManager.getInstance().areAllDownloadsComplete(server);
            if (!allFinished) {
                hasSeenActive = true;
                return ActionResult.BLOCK;
            }
            if (hasSeenActive) {
                hasSeenActive = false;
                graceTicks = 0;
                return ActionResult.SUCCESS;
            }
            graceTicks++;
            if (graceTicks < 10) {
                return ActionResult.BLOCK;
            }
            graceTicks = 0;
            return ActionResult.SUCCESS;
        } else {
            // "trigger"
            if (triggered) {
                triggered = false;
                return ActionResult.SUCCESS;
            }
            return ActionResult.BLOCK;
        }
    }

    @Override
    public String getType() {
        return "wait_until";
    }

    @Override
    public OrchestratorAction copy() {
        return new WaitUntilAction(this.waitType, this.ticks, this.triggerId, this.label);
    }
}
