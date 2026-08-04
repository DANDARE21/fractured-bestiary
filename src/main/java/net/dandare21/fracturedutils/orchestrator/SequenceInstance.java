package net.dandare21.fracturedutils.orchestrator;

import net.dandare21.fracturedutils.orchestrator.action.ActionResult;
import net.dandare21.fracturedutils.orchestrator.action.AwaitTriggerAction;
import net.dandare21.fracturedutils.orchestrator.action.OrchestratorAction;
import net.dandare21.fracturedutils.orchestrator.action.WaitUntilAction;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SequenceInstance {
    private final String sequenceName;
    private final String targetPlayerName;
    private final List<OrchestratorAction> actions;
    private final SequenceInstance parent;
    private final List<SequenceInstance> activeChildren = new CopyOnWriteArrayList<>();

    private int currentIndex = 0;
    private SequenceState state = SequenceState.RUNNING;

    public SequenceInstance(String sequenceName, String targetPlayerName, List<OrchestratorAction> actions, SequenceInstance parent) {
        this(sequenceName, targetPlayerName, actions, parent, 0);
    }

    public SequenceInstance(String sequenceName, String targetPlayerName, List<OrchestratorAction> actions, SequenceInstance parent, int startIndex) {
        this.sequenceName = sequenceName;
        this.targetPlayerName = targetPlayerName;
        this.actions = actions != null ? actions : new ArrayList<>();
        this.parent = parent;
        this.currentIndex = Math.max(0, Math.min(this.actions.size(), startIndex));
    }

    public SequenceInstance(String sequenceName, List<OrchestratorAction> actions, String targetPlayerName, SequenceInstance parent) {
        this(sequenceName, targetPlayerName, actions, parent, 0);
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public SequenceState getState() {
        return state;
    }

    public void setState(SequenceState state) {
        this.state = state;
    }

    public SequenceInstance getParent() {
        return parent;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = Math.max(0, Math.min(this.actions.size(), index));
    }

    public List<OrchestratorAction> getActions() {
        return actions;
    }

    public List<SequenceInstance> getActiveChildren() {
        return activeChildren;
    }

    public void addActiveChild(SequenceInstance child) {
        if (child != null) {
            activeChildren.add(child);
        }
    }

    public void pause() {
        if (this.state == SequenceState.RUNNING) {
            this.state = SequenceState.PAUSED;
        }
        for (SequenceInstance child : activeChildren) {
            child.pause();
        }
    }

    public void unpause() {
        if (this.state == SequenceState.PAUSED || this.state == SequenceState.STALLED) {
            this.state = SequenceState.RUNNING;
        }
        for (SequenceInstance child : activeChildren) {
            child.unpause();
        }
    }

    public void cancel() {
        this.state = SequenceState.FINISHED;
        for (SequenceInstance child : activeChildren) {
            child.cancel();
        }
        activeChildren.clear();
    }

    public void tick(MinecraftServer server) {
        if (state == SequenceState.FINISHED) {
            return;
        }

        // Tick active child nodes first
        for (SequenceInstance child : activeChildren) {
            child.tick(server);
            if (child.getState() == SequenceState.FINISHED) {
                activeChildren.remove(child);
            }
        }

        if (state == SequenceState.STALLED || state == SequenceState.PAUSED) {
            return;
        }

        // Execute current action
        while (currentIndex < actions.size()) {
            OrchestratorAction action = actions.get(currentIndex);
            ActionResult result = action.execute(this, server);

            if (result == ActionResult.SUCCESS) {
                currentIndex++;
            } else if (result == ActionResult.BLOCK) {
                // Action is waiting / blocking
                break;
            } else if (result == ActionResult.FAIL) {
                state = SequenceState.FINISHED;
                break;
            }
        }

        if (currentIndex >= actions.size()) {
            state = SequenceState.FINISHED;
        }
    }

    public boolean resumeTrigger(String triggerId) {
        boolean found = false;
        if (state == SequenceState.PAUSED || state == SequenceState.STALLED) {
            unpause();
            found = true;
        }

        boolean isWildcard = (triggerId == null || triggerId.isEmpty());

        if (currentIndex < actions.size()) {
            OrchestratorAction action = actions.get(currentIndex);
            if (action instanceof AwaitTriggerAction awaitAction) {
                if (isWildcard || awaitAction.getTriggerId().isEmpty() || awaitAction.getTriggerId().equalsIgnoreCase(triggerId)) {
                    awaitAction.trigger();
                    found = true;
                }
            } else if (action instanceof WaitUntilAction waitAction) {
                if (isWildcard || waitAction.getTriggerId().isEmpty() || waitAction.getTriggerId().equalsIgnoreCase(triggerId)) {
                    waitAction.trigger();
                    found = true;
                }
            }
        }

        for (SequenceInstance child : activeChildren) {
            if (child.resumeTrigger(triggerId)) {
                found = true;
            }
        }

        return found;
    }
}
