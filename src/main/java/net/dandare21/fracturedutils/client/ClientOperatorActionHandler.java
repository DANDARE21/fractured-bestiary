package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.C2SSubmitOperatorResumePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, value = Dist.CLIENT)
public class ClientOperatorActionHandler {
    private static final Map<String, String> activeOperatorActions = new HashMap<>();
    private static int holdTicks = 0;

    public static synchronized void updateActiveActions(Map<String, String> actionsMap) {
        activeOperatorActions.clear();
        if (actionsMap != null) {
            activeOperatorActions.putAll(actionsMap);
        }
        holdTicks = 0;
    }

    public static synchronized Map<String, String> getActiveOperatorActions() {
        return activeOperatorActions;
    }

    public static int getHoldTicks() {
        return holdTicks;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.player.hasPermissions(2) && !activeOperatorActions.isEmpty()) {
            if (ModKeyBindings.OPERATOR_RESUME_KEY.isDown()) {
                holdTicks++;
                if (holdTicks >= 20) {
                    // Holding completed -> send resume packet for first pending trigger
                    String triggerId = activeOperatorActions.keySet().iterator().next();
                    ModMessages.sendToServer(new C2SSubmitOperatorResumePacket(triggerId));
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.8f, 0.7f));
                    activeOperatorActions.remove(triggerId);
                    holdTicks = 0;
                }
            } else {
                holdTicks = Math.max(0, holdTicks - 1);
            }
        } else {
            holdTicks = 0;
        }
    }
}
