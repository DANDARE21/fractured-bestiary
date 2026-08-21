package net.dandare21.fracturedutils.puppet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

/**
 * Functional interface representing a discrete boss attack, spell, or scripted movement routine.
 */
@FunctionalInterface
public interface PuppetAction {
    /**
     * Executes the scripted action on the host entity.
     *
     * @param mob    The entity being puppeteered.
     * @param params Key-value parameters passed from the sequencer/data file.
     */
    void execute(Mob mob, CompoundTag params);
}
