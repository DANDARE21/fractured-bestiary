package net.dandare21.fracturedutils.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class TeamWipeScreen extends Screen {
    private static final int RED_BG = 0xF0140205;
    private static final int RED_ACCENT = 0xFFFF2244;
    private static final int RED_TEXT = 0xFFFF3355;

    private final int durationSeconds;
    private final long startTimeMs;

    public TeamWipeScreen(int durationSeconds) {
        super(Component.literal("Team Wiped"));
        this.durationSeconds = Math.max(1, durationSeconds);
        this.startTimeMs = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            if (this.minecraft.player != null) {
                net.dandare21.fracturedutils.client.camera.CameraUtils.setModernThirdPersonCamera(this.minecraft.player);
                this.minecraft.getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 0.8f, 0.5f)
                );
            }
            if (this.minecraft.gameRenderer != null) {
                try {
                    this.minecraft.gameRenderer.loadEffect(new net.minecraft.resources.ResourceLocation("shaders/post/desaturate.json"));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        net.dandare21.fracturedutils.client.camera.CustomCameraManager.clearCustomCamera();
        if (this.minecraft != null && this.minecraft.gameRenderer != null) {
            try {
                this.minecraft.gameRenderer.shutdownEffect();
            } catch (Exception ignored) {}
        }
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dark red background with glitch grid
        graphics.fill(0, 0, this.width, this.height, RED_BG);
        drawGridOverlay(graphics);

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        double remainingSec = Math.max(0.0, durationSeconds - (elapsedMs / 1000.0));

        if (remainingSec <= 0.0) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Banner box
        int boxW = 320;
        int boxH = 90;
        int boxX = centerX - (boxW / 2);
        int boxY = centerY - (boxH / 2);

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xEE120306);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + 2, RED_ACCENT);
        graphics.fill(boxX, boxY + boxH - 2, boxX + boxW, boxY + boxH, RED_ACCENT);
        graphics.fill(boxX, boxY, boxX + 2, boxY + boxH, RED_ACCENT);
        graphics.fill(boxX + boxW - 2, boxY, boxX + boxW, boxY + boxH, RED_ACCENT);

        // Header Title
        graphics.drawCenteredString(this.font, Component.literal("💀 TEAM WIPED").withStyle(ChatFormatting.BOLD), centerX, boxY + 14, RED_TEXT);

        // Subtitle
        graphics.drawCenteredString(this.font, Component.literal("Returning to Checkpoint...").withStyle(ChatFormatting.GOLD), centerX, boxY + 34, 0xFFFFAA00);

        // Timer
        String timerStr = String.format(java.util.Locale.US, "Respawning in %.1fs", remainingSec);
        graphics.drawCenteredString(this.font, Component.literal(timerStr).withStyle(ChatFormatting.GRAY), centerX, boxY + 56, 0xFFCCCCCC);
    }

    private void drawGridOverlay(GuiGraphics graphics) {
        int step = 20;
        int color = 0x0CFF0033;
        for (int x = 0; x < this.width; x += step) {
            graphics.fill(x, 0, x + 1, this.height, color);
        }
        for (int y = 0; y < this.height; y += step) {
            graphics.fill(0, y, this.width, y + 1, color);
        }
    }
}
