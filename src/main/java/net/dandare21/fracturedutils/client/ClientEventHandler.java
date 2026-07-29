package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.client.gui.WaitingRoomScreen;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.JoinWaitingRoomC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {

    public static int holdTicks = 0;
    public static final int MAX_HOLD_TICKS = 30; // 1.5 Seconds hold time

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            holdTicks = 0;
            return;
        }

        if (ClientWaitingRoomData.isActive() && mc.screen == null) {
            if (ModKeyBindings.WAITING_ROOM_KEY.isDown()) {
                holdTicks++;
                if (holdTicks >= MAX_HOLD_TICKS) {
                    holdTicks = 0;
                    ModMessages.sendToServer(new JoinWaitingRoomC2SPacket());
                    mc.setScreen(new WaitingRoomScreen());
                }
            } else {
                holdTicks = 0;
            }
        } else {
            holdTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            if (!ClientWaitingRoomData.isActive()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof WaitingRoomScreen) return;

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int width = mc.getWindow().getGuiScaledWidth();

            String keyName = ModKeyBindings.WAITING_ROOM_KEY.getTranslatedKeyMessage().getString();
            Component text = Component.literal("[Hold ")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(Component.literal(keyName).withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.BOLD))
                    .append(Component.literal("] Enter Event Waiting Room (No Going Back!)").withStyle(net.minecraft.ChatFormatting.GOLD));

            int textWidth = mc.font.width(text);
            int x = (width - textWidth) / 2;
            int y = 15;

            // Background card
            guiGraphics.fill(x - 8, y - 5, x + textWidth + 8, y + 20, 0xDD000000);
            guiGraphics.fill(x - 8, y - 5, x + textWidth + 8, y - 4, 0xFFFFAA00);
            guiGraphics.drawString(mc.font, text, x, y, 0xFFFFFF, true);

            // Progress bar directly below the text
            int barX = x;
            int barY = y + 13;
            int barWidth = textWidth;
            int barHeight = 4;

            float progress = (float) holdTicks / MAX_HOLD_TICKS;
            int filledWidth = (int) (barWidth * progress);

            // Bar background & fill
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x66555555);
            if (filledWidth > 0) {
                int color = progress >= 1.0f ? 0xFF00FF55 : 0xFFFFAA00;
                guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, color);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusClientEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(ModKeyBindings.WAITING_ROOM_KEY);
        }
    }
}
