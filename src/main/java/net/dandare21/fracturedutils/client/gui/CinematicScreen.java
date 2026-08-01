package net.dandare21.fracturedutils.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.dandare21.fracturedutils.client.ClientCutsceneHandler;
import net.dandare21.fracturedutils.client.ModKeyBindings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class CinematicScreen extends Screen {

    private int inputActivityTimer = 0;
    private int volumeActivityTimer = 0;
    private float skipHoldProgress = 0.0f;
    private float smoothSkipFadeAlpha = 0.0f;
    private float smoothVolumeFadeAlpha = 0.0f;

    private static final float MAX_SKIP_HOLD_TICKS = 60.0f; // 3.0 Seconds hold to skip

    public CinematicScreen() {
        super(Component.literal("Cinematic Playback"));
    }

    @Override
    protected void init() {
        super.init();
        hideMouseCursor();
    }

    private void hideMouseCursor() {
        if (this.minecraft != null) {
            long window = this.minecraft.getWindow().getWindow();
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    private void restoreMouseCursor() {
        if (this.minecraft != null) {
            long window = this.minecraft.getWindow().getWindow();
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    @Override
    public void onClose() {
        restoreMouseCursor();
        super.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void registerInputActivity() {
        this.inputActivityTimer = 50; // Keep GUI prompt visible for ~2.5s after input
    }

    private void registerVolumeActivity() {
        this.volumeActivityTimer = 45; // Keep Volume GUI visible for ~2.25s after adjustment
    }

    private boolean isSkipKeyDown() {
        if (this.minecraft == null) return false;
        long window = this.minecraft.getWindow().getWindow();
        InputConstants.Key key = ModKeyBindings.SKIP_CUTSCENE_KEY.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hideMouseCursor();

        // Draw solid black background frame
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);

        // Render full screen video frame via WaterMedia
        ClientCutsceneHandler cutsceneHandler = ClientCutsceneHandler.getInstance();
        cutsceneHandler.renderWaterMediaFrame(guiGraphics, this.width, this.height);

        // Handle Volume Adjustment Overlay Rendering
        if (volumeActivityTimer > 0) {
            volumeActivityTimer--;
        }
        boolean shouldShowVolume = volumeActivityTimer > 0;
        float targetVolAlpha = shouldShowVolume ? 1.0f : 0.0f;
        smoothVolumeFadeAlpha += (targetVolAlpha - smoothVolumeFadeAlpha) * 0.25f;

        if (smoothVolumeFadeAlpha > 0.02f) {
            renderVolumeGuiPrompt(guiGraphics, smoothVolumeFadeAlpha);
        }

        // Handle Cutscene Skip logic & rendering if skip is allowed
        if (cutsceneHandler.isAllowSkip()) {
            boolean isHoldingSkip = isSkipKeyDown();

            if (isHoldingSkip) {
                registerInputActivity();
                skipHoldProgress += 1.0f / MAX_SKIP_HOLD_TICKS;
                if (skipHoldProgress >= 1.0f) {
                    skipHoldProgress = 1.0f;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.4f, 0.6f));
                    }
                    cutsceneHandler.stopAndCleanup();
                    return;
                }
            } else {
                skipHoldProgress = Math.max(0.0f, skipHoldProgress - (2.0f / MAX_SKIP_HOLD_TICKS));
            }

            if (inputActivityTimer > 0) {
                inputActivityTimer--;
            }

            // Smooth fade alpha for skip GUI
            boolean shouldShowSkip = inputActivityTimer > 0 || skipHoldProgress > 0.0f;
            float targetSkipAlpha = shouldShowSkip ? 1.0f : 0.0f;
            smoothSkipFadeAlpha += (targetSkipAlpha - smoothSkipFadeAlpha) * 0.25f;

            if (smoothSkipFadeAlpha > 0.02f) {
                renderSkipGuiPrompt(guiGraphics, smoothSkipFadeAlpha);
            }
        }
    }

    private void renderVolumeGuiPrompt(GuiGraphics guiGraphics, float alpha) {
        if (this.minecraft == null) return;

        int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));
        int alphaMask = alphaInt << 24;

        int volPercent = ClientCutsceneHandler.getInstance().getUserVolumePercent();

        String volTextStr = volPercent == 0 ? "MUTED" : "VOL " + volPercent + "%";
        Component volText = Component.literal(volTextStr).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA);

        int textW = this.font.width(volText);
        int cardW = Math.max(125, textW + 45);
        int cardH = 24;
        int x = this.width - cardW - 16;
        int y = 16;

        int fillColor = alphaMask | 0x08121B;
        int borderColor = alphaMask | 0x00E5FF;

        // Background card fill & border
        guiGraphics.fill(x, y, x + cardW, y + cardH, fillColor);
        guiGraphics.fill(x, y, x + cardW, y + 1, borderColor);
        guiGraphics.fill(x, y + cardH - 1, x + cardW, y + cardH, borderColor);
        guiGraphics.fill(x, y, x + 1, y + cardH, borderColor);
        guiGraphics.fill(x + cardW - 1, y, x + cardW, y + cardH, borderColor);

        // Corner accents
        guiGraphics.fill(x + cardW - 4, y, x + cardW, y + 2, borderColor);
        guiGraphics.fill(x, y, x + 4, y + 2, borderColor);

        // Draw Volume Text on left
        guiGraphics.drawString(this.font, volText, x + 8, y + 8, alphaMask | 0xFFFFFF);

        // Draw Mini Volume Bar on right
        int barX = x + textW + 14;
        int barY = y + 9;
        int barWidth = cardW - (textW + 20);
        int barHeight = 6;

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, alphaMask | 0x050B10);

        int filledW = (int) (barWidth * (volPercent / 100.0f));
        if (filledW > 0) {
            guiGraphics.fill(barX, barY, barX + filledW, barY + barHeight, borderColor);
        }
    }

    private void renderSkipGuiPrompt(GuiGraphics guiGraphics, float alpha) {
        if (this.minecraft == null) return;

        int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));
        int alphaMask = alphaInt << 24;

        String keyName = ModKeyBindings.SKIP_CUTSCENE_KEY.getTranslatedKeyMessage().getString().toUpperCase();
        Component promptText = Component.literal("HOLD ").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA)
                .append(Component.literal("[" + keyName + "]").withStyle(ChatFormatting.BOLD, ChatFormatting.WHITE))
                .append(Component.literal(" TO SKIP").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA));

        int textW = this.font.width(promptText);
        int cardW = Math.max(160, textW + 36);
        int cardH = 34;
        int x = (this.width - cardW) / 2;
        int y = this.height - cardH - 24;

        int fillColor = alphaMask | 0x08121B;
        int borderColor = alphaMask | 0x00E5FF;

        // Draw Card Background Fill & Borders
        guiGraphics.fill(x, y, x + cardW, y + cardH, fillColor);
        guiGraphics.fill(x, y, x + cardW, y + 1, borderColor);
        guiGraphics.fill(x, y + cardH - 1, x + cardW, y + cardH, borderColor);
        guiGraphics.fill(x, y, x + 1, y + cardH, borderColor);
        guiGraphics.fill(x + cardW - 1, y, x + cardW, y + cardH, borderColor);

        // Corner accents
        guiGraphics.fill(x, y, x + 4, y + 2, borderColor);
        guiGraphics.fill(x + cardW - 4, y, x + cardW, y + 2, borderColor);

        // Centered prompt text
        guiGraphics.drawCenteredString(this.font, promptText, x + (cardW / 2), y + 7, alphaMask | 0xFFFFFF);

        // Progress Bar
        int barX = x + 6;
        int barY = y + cardH - 7;
        int barWidth = cardW - 12;
        int barHeight = 3;

        // Bar Track Background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, alphaMask | 0x050B10);

        int filledWidth = (int) (barWidth * Math.max(0.0f, Math.min(1.0f, skipHoldProgress)));
        if (filledWidth > 0) {
            int progressColor = skipHoldProgress >= 0.95f ? (alphaMask | 0x00FF55) : borderColor;
            guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, progressColor);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        registerInputActivity();

        if (keyCode == GLFW.GLFW_KEY_UP) {
            registerVolumeActivity();
            ClientCutsceneHandler.getInstance().adjustUserVolume(5);
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.6f, 0.4f));
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            registerVolumeActivity();
            ClientCutsceneHandler.getInstance().adjustUserVolume(-5);
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.2f, 0.4f));
            }
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (this.minecraft != null) {
                restoreMouseCursor();
                ClientCutsceneHandler.getInstance().setInEscMenu(true);
                this.minecraft.setScreen(new PauseScreen(true));
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        registerInputActivity();
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        registerInputActivity();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        registerInputActivity();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        registerInputActivity();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        registerInputActivity();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        registerInputActivity();
        return true;
    }
}
