package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.client.camera.CameraUtils;
import net.dandare21.fracturedutils.client.camera.CustomCameraManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DownedSpectateScreen extends Screen {
    private int spectateIndex = 0;
    private float smoothReviveProgress = 0.0f;

    public DownedSpectateScreen() {
        super(Component.literal("Downed Spectate Screen"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        List<AbstractClientPlayer> players = getCandidatePlayers();
        if (!players.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                spectateIndex = (spectateIndex - 1 + players.size()) % players.size();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                spectateIndex = (spectateIndex + 1) % players.size();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private List<AbstractClientPlayer> getCandidatePlayers() {
        List<AbstractClientPlayer> candidates = new ArrayList<>();
        if (this.minecraft != null && this.minecraft.level != null) {
            for (AbstractClientPlayer p : this.minecraft.level.players()) {
                if (!p.isSpectator()) {
                    candidates.add(p);
                }
            }
        }
        return candidates;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<AbstractClientPlayer> candidates = getCandidatePlayers();
        if (candidates.isEmpty()) return;

        if (spectateIndex >= candidates.size()) {
            spectateIndex = 0;
        }

        AbstractClientPlayer target = candidates.get(spectateIndex);
        if (target != null) {
            CameraUtils.setModernThirdPersonCamera(target);
        }

        int centerX = this.width / 2;

        // Top Banner Indicator
        graphics.fill(centerX - 130, 10, centerX + 130, 42, 0xDD0C131D);
        graphics.fill(centerX - 130, 10, centerX + 130, 12, 0xFF00E5FF);
        graphics.fill(centerX - 130, 40, centerX + 130, 42, 0xFF00E5FF);

        graphics.drawCenteredString(this.font, Component.literal("SYSTEM DOWNED - SPECTATING").withStyle(ChatFormatting.BOLD), centerX, 16, 0xFFFF3355);
        graphics.drawCenteredString(this.font, Component.literal("Press  <-  or  ->  Arrow Keys to Switch Target").withStyle(ChatFormatting.GRAY), centerX, 28, 0xFFAABBCC);

        // Smoothly interpolate revive progress
        float targetProg = net.dandare21.fracturedutils.client.ClientDownedData.getReviveProgress();
        if (Math.abs(smoothReviveProgress - targetProg) > 0.0001f) {
            smoothReviveProgress += (targetProg - smoothReviveProgress) * 0.2f;
            if (targetProg == 0.0f && smoothReviveProgress < 0.005f) {
                smoothReviveProgress = 0.0f;
            }
        }

        // Revive Progress Overlay when being revived
        if (smoothReviveProgress > 0.001f) {
            int boxW = 220;
            int boxH = 40;
            int boxX = centerX - (boxW / 2);
            int boxY = 48;

            // 1. Dark Base Panel Fill
            graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xEE051A0B);

            // 2. Cyberpunk Borders & Content
            graphics.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF00FF55);
            graphics.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0xFF00FF55);
            graphics.fill(boxX, boxY, boxX + 1, boxY + boxH, 0xFF00FF55);
            graphics.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, 0xFF00FF55);

            String text = String.format(java.util.Locale.US, "BEING REVIVED  //  %d%%", (int)(smoothReviveProgress * 100));
            graphics.drawCenteredString(this.font, Component.literal(text).withStyle(ChatFormatting.BOLD), centerX, boxY + 7, 0xFF00FF55);

            int trackX = boxX + 10;
            int trackY = boxY + boxH - 12;
            int trackW = boxW - 20;
            int trackH = 5;

            graphics.fill(trackX - 1, trackY - 1, trackX + trackW + 1, trackY + trackH + 1, 0x6600FF55);
            graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF02160C);
            int fillW = (int) (trackW * smoothReviveProgress);
            if (fillW > 0) {
                graphics.fill(trackX, trackY, trackX + fillW, trackY + trackH, 0xFF00FF55);
                graphics.fill(trackX + Math.max(0, fillW - 3), trackY, trackX + fillW, trackY + trackH, 0xFFFFFFFF);
            }
        }

        // Bottom Player Heads Bar
        int headSize = 24;
        int spacing = 48;
        int totalW = candidates.size() * spacing;
        int startX = centerX - (totalW / 2) + (spacing / 2) - (headSize / 2);
        int barY = this.height - 55;

        // HUD Bar background panel
        graphics.fill(centerX - (totalW / 2) - 15, barY - 8, centerX + (totalW / 2) + 15, barY + 45, 0xEE08121B);
        graphics.fill(centerX - (totalW / 2) - 15, barY - 8, centerX + (totalW / 2) + 15, barY - 6, 0xAA00E5FF);

        for (int i = 0; i < candidates.size(); i++) {
            AbstractClientPlayer p = candidates.get(i);
            int itemX = startX + (i * spacing);
            boolean isSelected = (i == spectateIndex);

            ResourceLocation skin = p.getSkinTextureLocation();
            PlayerFaceRenderer.draw(graphics, skin, itemX, barY, headSize);

            // Active Target Frame
            if (isSelected) {
                graphics.fill(itemX - 3, barY - 3, itemX + headSize + 3, barY - 1, 0xFF00E5FF);
                graphics.fill(itemX - 3, barY + headSize + 1, itemX + headSize + 3, barY + headSize + 3, 0xFF00E5FF);
                graphics.fill(itemX - 3, barY - 3, itemX - 1, barY + headSize + 3, 0xFF00E5FF);
                graphics.fill(itemX + headSize + 1, barY - 3, itemX + headSize + 3, barY + headSize + 3, 0xFF00E5FF);
            }

            // Player Name Label
            String nameStr = p.getScoreboardName();
            if (nameStr.length() > 7) {
                nameStr = nameStr.substring(0, 6) + "..";
            }
            int nameColor = isSelected ? 0xFF00E5FF : 0xFFAABBCC;
            graphics.drawCenteredString(this.font, nameStr, itemX + (headSize / 2), barY + headSize + 6, nameColor);
        }
    }

    @Override
    public void removed() {
        super.removed();
        CustomCameraManager.clearCustomCamera();
    }
}
