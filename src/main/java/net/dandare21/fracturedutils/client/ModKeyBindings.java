package net.dandare21.fracturedutils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY_FRACTURED_UTILS = "key.categories.fractured_utils";
    public static final String KEY_WAITING_ROOM = "key.fractured_utils.waiting_room";
    public static final String KEY_SKIP_CUTSCENE = "key.fractured_utils.skip_cutscene";
    public static final String KEY_OPERATOR_RESUME = "key.fractured_utils.operator_resume";
    public static final String KEY_DIALOG_ADVANCE = "key.fractured_utils.dialog_advance";

    public static final KeyMapping WAITING_ROOM_KEY = new KeyMapping(
            KEY_WAITING_ROOM,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            KEY_CATEGORY_FRACTURED_UTILS
    );

    public static final KeyMapping SKIP_CUTSCENE_KEY = new KeyMapping(
            KEY_SKIP_CUTSCENE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            KEY_CATEGORY_FRACTURED_UTILS
    );

    public static final KeyMapping OPERATOR_RESUME_KEY = new KeyMapping(
            KEY_OPERATOR_RESUME,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            KEY_CATEGORY_FRACTURED_UTILS
    );

    public static final KeyMapping DIALOG_ADVANCE_KEY = new KeyMapping(
            KEY_DIALOG_ADVANCE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E, // Key E by default!
            KEY_CATEGORY_FRACTURED_UTILS
    );
}
