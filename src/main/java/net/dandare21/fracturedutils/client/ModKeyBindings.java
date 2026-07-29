package net.dandare21.fracturedutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY_FRACTURED_UTILS = "key.categories.fractured_utils";
    public static final String KEY_WAITING_ROOM = "key.fractured_utils.waiting_room";

    public static final KeyMapping WAITING_ROOM_KEY = new KeyMapping(
            KEY_WAITING_ROOM,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            KEY_CATEGORY_FRACTURED_UTILS
    );
}
