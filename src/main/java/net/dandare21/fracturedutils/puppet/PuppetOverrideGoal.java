package net.dandare21.fracturedutils.puppet;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Priority 0 goal that claims standard AI control flags, suppressing vanilla and modded goals while puppeting is active.
 */
public class PuppetOverrideGoal extends Goal {
    private final Mob mob;
    private final PuppetController controller;

    public PuppetOverrideGoal(Mob mob, PuppetController controller) {
        this.mob = mob;
        this.controller = controller;
        // Suppress all standard AI sub-systems when active
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.controller.isPuppetingActive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.controller.isPuppetingActive();
    }

    @Override
    public void tick() {
        this.controller.tick();
    }
}
