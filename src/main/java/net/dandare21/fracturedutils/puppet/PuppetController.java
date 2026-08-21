package net.dandare21.fracturedutils.puppet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller component stored inside a puppet entity. Manages AI aspect suppression flags,
 * registered actions, and active action lifetimes.
 */
public class PuppetController {
    private final Mob mob;
    private final Map<ResourceLocation, PuppetAction> actions = new HashMap<>();

    // AI Aspect Suppression Flags
    private boolean suppressNavigation = false;
    private boolean suppressTargeting = false;
    private boolean suppressLook = false;
    private boolean puppetingActive = false;

    // Active Action State
    private int actionTicksRemaining = 0;
    private Runnable onActionCompleteCallback = null;

    public PuppetController(Mob mob) {
        this.mob = mob;
        // Inject the priority 0 hijack goal into the mob's goal selector
        this.mob.goalSelector.addGoal(0, new PuppetOverrideGoal(this.mob, this));
    }

    // --- Registration & Execution ---

    public void registerAction(ResourceLocation id, PuppetAction action) {
        this.actions.put(id, action);
    }

    public void executeAction(ResourceLocation actionId, CompoundTag params, int durationTicks, Runnable onComplete) {
        PuppetAction action = this.actions.get(actionId);
        if (action != null) {
            this.setPuppetingActive(true);
            this.actionTicksRemaining = durationTicks;
            this.onActionCompleteCallback = onComplete;
            action.execute(this.mob, params != null ? params : new CompoundTag());
        }
    }

    public void executeAction(ResourceLocation actionId, CompoundTag params) {
        executeAction(actionId, params, 0, null);
    }

    public void executeAction(ResourceLocation actionId, CompoundTag params, int durationTicks) {
        executeAction(actionId, params, durationTicks, null);
    }

    // --- Tick Update ---

    public void tick() {
        if (!this.puppetingActive) return;

        if (this.actionTicksRemaining > 0) {
            this.actionTicksRemaining--;
            if (this.actionTicksRemaining <= 0) {
                this.stopAction();
            }
        }
    }

    public void stopAction() {
        this.puppetingActive = false;
        this.actionTicksRemaining = 0;
        this.resetSuppressionFlags();
        if (this.onActionCompleteCallback != null) {
            Runnable callback = this.onActionCompleteCallback;
            this.onActionCompleteCallback = null;
            callback.run();
        }
    }

    // --- Suppression Controls ---

    public void setSuppressNavigation(boolean suppress) {
        this.suppressNavigation = suppress;
        if (suppress) this.mob.getNavigation().stop();
    }

    public void setSuppressTargeting(boolean suppress) {
        this.suppressTargeting = suppress;
        if (suppress) this.mob.setTarget(null);
    }

    public void setSuppressLook(boolean suppress) {
        this.suppressLook = suppress;
    }

    public void setPuppetingActive(boolean active) {
        this.puppetingActive = active;
        if (!active) {
            this.resetSuppressionFlags();
        }
    }

    public void resetSuppressionFlags() {
        this.suppressNavigation = false;
        this.suppressTargeting = false;
        this.suppressLook = false;
    }

    // --- Primitive Direct Controls (For Sequencer Scripts) ---

    public void forceMoveTo(double x, double y, double z, double speed) {
        this.mob.getNavigation().moveTo(x, y, z, speed);
    }

    public void forceLookAt(Entity target) {
        this.mob.getLookControl().setLookAt(target, 360.0F, 360.0F);
    }

    public void forceLookAt(double x, double y, double z) {
        this.mob.getLookControl().setLookAt(x, y, z, 360.0F, 360.0F);
    }

    // --- Getters ---

    public Mob getMob() { return this.mob; }
    public Map<ResourceLocation, PuppetAction> getActions() { return Collections.unmodifiableMap(this.actions); }
    public boolean isPuppetingActive() { return this.puppetingActive; }
    public boolean isNavigationSuppressed() { return this.suppressNavigation; }
    public boolean isTargetingSuppressed() { return this.suppressTargeting; }
    public boolean isLookSuppressed() { return this.suppressLook; }
}
