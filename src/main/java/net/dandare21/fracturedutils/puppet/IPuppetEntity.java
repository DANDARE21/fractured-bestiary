package net.dandare21.fracturedutils.puppet;

/**
 * Implemented by any entity (Mob, PathfinderMob, GeoEntity) that can be controlled by the sequencer.
 */
public interface IPuppetEntity {
    /**
     * @return The entity's persistent PuppetController instance.
     */
    PuppetController getPuppetController();

    /**
     * Called during entity initialization to register custom actions.
     *
     * @param registry Builder registry mapping ResourceLocations to PuppetActions.
     */
    default void registerPuppetActions(PuppetActionRegistry registry) {
        // Optional override for custom mob actions
    }
}
