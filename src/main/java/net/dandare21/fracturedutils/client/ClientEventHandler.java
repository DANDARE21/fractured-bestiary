package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.client.gui.WaitingRoomScreen;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.JoinWaitingRoomC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
    public static int ticksSinceLastSound = 0;
    public static final int MAX_HOLD_TICKS = 30; // 1.5 Seconds hold time
    public static float smoothHoldProgress = 0.0f;

    @SubscribeEvent
    public static void onClientChat(ClientChatReceivedEvent event) {
        ClientWaitingRoomData.addChatMessage(event.getMessage());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            holdTicks = 0;
            ticksSinceLastSound = 0;
            return;
        }

        boolean isOp = mc.player.hasPermissions(2);

        if (ClientWaitingRoomData.isActive() && mc.screen == null) {
            if (isOp) {
                if (ModKeyBindings.WAITING_ROOM_KEY.consumeClick()) {
                    holdTicks = 0;
                    ticksSinceLastSound = 0;
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4f, 0.6f));
                    ModMessages.sendToServer(new JoinWaitingRoomC2SPacket());
                    mc.setScreen(new WaitingRoomScreen());
                }
            } else if (ModKeyBindings.WAITING_ROOM_KEY.isDown()) {
                holdTicks++;
                ticksSinceLastSound++;

                float progress = (float) holdTicks / MAX_HOLD_TICKS;
                // High rate at start (every 1 tick), decelerating as progress increases (up to every 5 ticks)
                int targetInterval = Math.max(1, Math.round(1.0f + (progress * 4.0f)));

                if (ticksSinceLastSound >= targetInterval) {
                    ticksSinceLastSound = 0;
                    float pitch = 0.8f + (progress * 0.8f);
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.3f));
                }

                if (holdTicks >= MAX_HOLD_TICKS) {
                    holdTicks = 0;
                    ticksSinceLastSound = 0;
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4f, 0.6f));
                    ModMessages.sendToServer(new JoinWaitingRoomC2SPacket());
                    mc.setScreen(new WaitingRoomScreen());
                }
            } else {
                holdTicks = 0;
                ticksSinceLastSound = 0;
            }
        } else {
            holdTicks = 0;
            ticksSinceLastSound = 0;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            if (!ClientWaitingRoomData.isActive()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof WaitingRoomScreen) return;

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();

            int totalConnected = 0;
            if (mc.getConnection() != null && mc.getConnection().getOnlinePlayers() != null) {
                totalConnected = mc.getConnection().getOnlinePlayers().size();
            }

            boolean isOp = mc.player != null && mc.player.hasPermissions(2);
            boolean isCountdown = ClientWaitingRoomData.isCountingDown();
            boolean isEveryoneReady = ClientWaitingRoomData.isEveryoneReady(totalConnected);

            int borderColor = isCountdown ? 0xFFFF3355 : (isEveryoneReady ? 0xFF00FF55 : 0xFF00E5FF);
            int innerColor = isCountdown ? 0x44FF3355 : (isEveryoneReady ? 0x4400FF55 : 0x4400E5FF);
            int titleColor = isCountdown ? 0xFFFF3355 : (isEveryoneReady ? 0xFF00FF55 : 0xFF00E5FF);
            net.minecraft.ChatFormatting titleStyle = isCountdown ? net.minecraft.ChatFormatting.RED : (isEveryoneReady ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.AQUA);
            net.minecraft.ChatFormatting statsStyle = isCountdown ? net.minecraft.ChatFormatting.RED : (isEveryoneReady ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.YELLOW);
            net.minecraft.ChatFormatting promptStyle = isCountdown ? net.minecraft.ChatFormatting.RED : (isEveryoneReady ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.AQUA);

            // Line 1: Event Title
            String titleStr = ClientWaitingRoomData.getRoomTitle().toUpperCase();
            Component titleText = Component.literal("★ " + titleStr + " ★").withStyle(net.minecraft.ChatFormatting.BOLD, titleStyle);

            // Line 2: Player count & timer/countdown
            int joinedCount = ClientWaitingRoomData.getPlayerUUIDs().size();

            long remaining = isCountdown ? ClientWaitingRoomData.getCountdownRemainingSeconds() : 0;
            String timeStr = isCountdown
                    ? Component.translatable("gui.fracturedutils.waiting_room.starting_in", remaining / 60, remaining % 60).getString()
                    : String.format("⏱ %02d:%02d", ClientWaitingRoomData.getElapsedSeconds() / 60, ClientWaitingRoomData.getElapsedSeconds() % 60);

            Component statsText = Component.translatable("gui.fracturedutils.waiting_room.hud_players", joinedCount, totalConnected)
                    .append(Component.literal("   |   " + timeStr))
                    .withStyle(statsStyle, net.minecraft.ChatFormatting.BOLD);

            // Line 3: Key prompt
            String keyName = ModKeyBindings.WAITING_ROOM_KEY.getTranslatedKeyMessage().getString().toUpperCase();
            Component promptPrefix = Component.translatable(isOp ? "gui.fracturedutils.waiting_room.press" : "gui.fracturedutils.waiting_room.hold");
            Component enterPrompt = Component.translatable("gui.fracturedutils.waiting_room.enter_prompt");

            Component promptText = Component.literal("[").withStyle(promptStyle, net.minecraft.ChatFormatting.BOLD)
                    .append(promptPrefix.getString() + " ")
                    .append(Component.literal(keyName).withStyle(net.minecraft.ChatFormatting.WHITE, net.minecraft.ChatFormatting.BOLD))
                    .append(enterPrompt)
                    .withStyle(promptStyle, net.minecraft.ChatFormatting.BOLD);

            int w1 = mc.font.width(titleText);
            int w2 = mc.font.width(statsText);
            int w3 = mc.font.width(promptText);
            int maxW = Math.max(w1, Math.max(w2, w3));

            int cardW = maxW + 32;
            int cardH = 48;
            int x = (screenWidth - cardW) / 2;
            int y = 10;

            int fillColor = 0xEE08121B;

            // Cyberpunk Card Background Fill & Primary Outer Borders
            guiGraphics.fill(x, y, x + cardW, y + cardH, fillColor);
            guiGraphics.fill(x, y, x + cardW, y + 1, borderColor);
            guiGraphics.fill(x, y + cardH - 1, x + cardW, y + cardH, borderColor);
            guiGraphics.fill(x, y, x + 1, y + cardH, borderColor);
            guiGraphics.fill(x + cardW - 1, y, x + cardW, y + cardH, borderColor);

            // Cyberpunk Double Inner Border Line
            guiGraphics.fill(x + 2, y + 2, x + cardW - 2, y + 3, innerColor);
            guiGraphics.fill(x + 2, y + cardH - 3, x + cardW - 2, y + cardH - 2, innerColor);

            // Hover corner notch accents
            guiGraphics.fill(x, y, x + 5, y + 2, borderColor);
            guiGraphics.fill(x, y, x + 2, y + 5, borderColor);
            guiGraphics.fill(x + cardW - 5, y, x + cardW, y + 2, borderColor);
            guiGraphics.fill(x + cardW - 2, y, x + cardW, y + 5, borderColor);
            guiGraphics.fill(x, y + cardH - 2, x + 5, y + cardH, borderColor);
            guiGraphics.fill(x, y + cardH - 5, x + 2, y + cardH, borderColor);
            guiGraphics.fill(x + cardW - 5, y + cardH - 2, x + cardW, y + cardH, borderColor);
            guiGraphics.fill(x + cardW - 2, y + cardH - 5, x + cardW, y + cardH, borderColor);

            // Draw Lines centered
            guiGraphics.drawCenteredString(mc.font, titleText, x + (cardW / 2), y + 6, titleColor);
            guiGraphics.drawCenteredString(mc.font, statsText, x + (cardW / 2), y + 19, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(mc.font, promptText, x + (cardW / 2), y + 32, 0xFFFFFFFF);

            // Smooth Progress Bar Calculation
            float partialTick = event.getPartialTick();
            if (holdTicks > 0) {
                float rawProgress = Math.min(1.0f, (holdTicks + (ModKeyBindings.WAITING_ROOM_KEY.isDown() ? partialTick : -partialTick)) / MAX_HOLD_TICKS);
                smoothHoldProgress = smoothHoldProgress + (rawProgress - smoothHoldProgress) * 0.4f;
            } else {
                smoothHoldProgress = smoothHoldProgress * 0.6f;
            }

            int barX = x + 4;
            int barY = y + cardH - 3;
            int barWidth = cardW - 8;
            int barHeight = 2;

            // Background bar track
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x55050B10);

            int filledWidth = (int) (barWidth * Math.max(0.0f, Math.min(1.0f, smoothHoldProgress)));
            if (filledWidth > 0) {
                int progressColor = smoothHoldProgress >= 0.95f ? 0xFF00FF55 : borderColor;
                guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, progressColor);
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
