package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.client.ClientWaitingRoomData;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.ToggleReadyC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class WaitingRoomScreen extends Screen {

    public WaitingRoomScreen() {
        super(Component.literal("Event Waiting Room"));
    }

    private boolean isOp() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Non-OP players cannot close the screen using ESC while waiting room is active
        return isOp();
    }

    @Override
    public void onClose() {
        if (isOp() || !ClientWaitingRoomData.isActive()) {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 180;
        int buttonHeight = 22;
        int centerX = this.width / 2;
        int bottomY = this.height - 38;

        UUID selfUUID = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
        boolean selfReady = ClientWaitingRoomData.isSelfReady(selfUUID);

        // Ready / Unready Toggle Button for BOTH OPs and Non-OPs
        Component readyBtnLabel = selfReady
                ? Component.literal("STATUS: [✔ READY]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.literal("STATUS: [✖ NOT READY]").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);

        this.addRenderableWidget(Button.builder(
                readyBtnLabel,
                button -> {
                    ModMessages.sendToServer(new ToggleReadyC2SPacket());
                }
        ).bounds(isOp() ? centerX - buttonWidth - 10 : centerX - (buttonWidth / 2), bottomY, buttonWidth, buttonHeight).build());

        // Admin-only Close button
        if (isOp()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Close Screen (Admin)").withStyle(ChatFormatting.RED),
                    button -> super.onClose()
            ).bounds(centerX + 10, bottomY, buttonWidth, buttonHeight).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fullscreen dark glassmorphic background
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE0B0E14);

        // Decorative top & bottom headers
        guiGraphics.fill(0, 0, this.width, 4, 0xFFFFAA00);
        guiGraphics.fill(0, 4, this.width, 24, 0x44FFAA00);
        guiGraphics.fill(0, this.height - 4, this.width, this.height, 0xFFFFAA00);
        guiGraphics.fill(0, this.height - 24, this.width, this.height - 4, 0x44FFAA00);

        // Header Title Banner
        guiGraphics.drawCenteredString(this.font,
                Component.literal("★ EVENT WAITING ROOM ★").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD),
                this.width / 2, 10, 0xFFFFAA00);

        // Main Fullscreen Card Container
        int marginX = Math.max(20, (this.width - 520) / 2);
        int cardTop = 34;
        int cardBottom = this.height - 46;
        int cardWidth = this.width - (marginX * 2);
        int cardHeight = cardBottom - cardTop;

        // Card background & gold accent borders
        guiGraphics.fill(marginX, cardTop, marginX + cardWidth, cardBottom, 0xDD121722);
        guiGraphics.fill(marginX, cardTop, marginX + cardWidth, cardTop + 2, 0xFFFFAA00);
        guiGraphics.fill(marginX, cardBottom - 2, marginX + cardWidth, cardBottom, 0xFFFFAA00);
        guiGraphics.fill(marginX, cardTop, marginX + 2, cardBottom, 0xFFFFAA00);
        guiGraphics.fill(marginX + cardWidth - 2, cardTop, marginX + cardWidth, cardBottom, 0xFFFFAA00);

        // "STARTING SOON..." Title
        guiGraphics.drawCenteredString(this.font,
                Component.literal("STARTING SOON...").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW),
                this.width / 2, cardTop + 12, 0xFFFF55);

        // Room Subtitle / Event Name
        String roomTitle = ClientWaitingRoomData.getRoomTitle();
        guiGraphics.drawCenteredString(this.font,
                Component.literal("Event: " + roomTitle).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),
                this.width / 2, cardTop + 26, 0x55FFFF);

        // Separator line
        guiGraphics.fill(marginX + 20, cardTop + 40, marginX + cardWidth - 20, cardTop + 41, 0x55FFFFFF);

        // Joined Players Header
        List<String> playerNames = ClientWaitingRoomData.getPlayerNames();
        List<Boolean> readyStates = ClientWaitingRoomData.getPlayerReadyStates();
        int count = playerNames.size();
        guiGraphics.drawString(this.font,
                Component.literal("Players In Waiting Room (" + count + "):").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                marginX + 25, cardTop + 48, 0x55FF55, true);

        // Player List Box
        int listBoxTop = cardTop + 62;
        int listBoxBottom = cardBottom - 38;
        int listBoxLeft = marginX + 20;
        int listBoxWidth = cardWidth - 40;
        int listBoxHeight = listBoxBottom - listBoxTop;

        guiGraphics.fill(listBoxLeft, listBoxTop, listBoxLeft + listBoxWidth, listBoxBottom, 0x88000000);

        if (playerNames.isEmpty()) {
            guiGraphics.drawCenteredString(this.font,
                    Component.literal("Waiting for players to enter...").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    this.width / 2, listBoxTop + (listBoxHeight / 2) - 4, 0x888888);
        } else {
            int columns = Math.max(1, listBoxWidth / 180);
            int colWidth = (listBoxWidth - (10 * (columns + 1))) / columns;
            int yOffset = 8;

            for (int i = 0; i < playerNames.size(); i++) {
                int col = i % columns;
                int row = i / columns;

                int xPos = listBoxLeft + 10 + (col * (colWidth + 10));
                int yPos = listBoxTop + yOffset + (row * 22);

                if (yPos + 20 > listBoxBottom) {
                    break;
                }

                String name = playerNames.get(i);
                boolean isReady = i < readyStates.size() && readyStates.get(i);

                int cardBg = isReady ? 0x66113311 : 0x66332211;
                int barColor = isReady ? 0xFF00FF55 : 0xFFFFAA00;
                String statusTag = isReady ? "[READY]" : "[NOT READY]";
                ChatFormatting statusFormat = isReady ? ChatFormatting.GREEN : ChatFormatting.YELLOW;

                guiGraphics.fill(xPos, yPos, xPos + colWidth, yPos + 18, cardBg);
                guiGraphics.fill(xPos, yPos, xPos + 3, yPos + 18, barColor);
                guiGraphics.drawString(this.font,
                        Component.literal(name + " ").withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(statusTag).withStyle(statusFormat)),
                        xPos + 8, yPos + 5, 0xFFFFFF, false);
            }
        }

        // Non-OP Locked Notice Banner at Bottom
        if (!isOp()) {
            Component lockedNotice = Component.literal("🔒 Screen locked — Waiting for an admin to start the event...")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            guiGraphics.drawCenteredString(this.font, lockedNotice, this.width / 2, this.height - 16, 0xFF5555);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
