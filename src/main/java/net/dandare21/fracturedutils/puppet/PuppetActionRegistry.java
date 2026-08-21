package net.dandare21.fracturedutils.puppet;

import net.minecraft.resources.ResourceLocation;

/**
 * Registry wrapper used during entity initialization to register PuppetActions.
 */
public class PuppetActionRegistry {
    private final PuppetController controller;

    public PuppetActionRegistry(PuppetController controller) {
        this.controller = controller;
    }

    /**
     * Registers a puppet action with a given ResourceLocation ID.
     *
     * @param id     Unique identifier for the action.
     * @param action The action callback to execute.
     */
    public void register(ResourceLocation id, PuppetAction action) {
        this.controller.registerAction(id, action);
    }

    public PuppetController getController() {
        return controller;
    }
}
