package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.dialog.DialogFormatUtil;
import net.dandare21.fracturedutils.dialog.DialogLine;
import net.dandare21.fracturedutils.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EditDialogLineModalScreen extends Screen {
    private static final int CYAN_MAIN = 0xFF00E5FF;
    private static final int CYAN_BG = 0xFF05090C;
    private static final int RED_CANCEL = 0xFFFF3355;

    private final Screen parentScreen;
    private final DialogLine line;
    private final Consumer<DialogLine> onSave;

    private EditBox speakerBox;
    private EditBox textBox;
    private EditBox delayBox;
    private EditBox soundBox;
    private EditBox speedBox;
    private EditBox letterSoundBox;
    private EditBox letterPitchMinBox;
    private EditBox letterPitchMaxBox;
    private CyberpunkDropdown<String> letterSoundDropdown;
    private CyberpunkCheckbox waitForInputCheckbox;
    private CyberpunkButton cameraSetupBtn;

    public EditDialogLineModalScreen(Screen parentScreen, DialogLine line, Consumer<DialogLine> onSave) {
        super(Component.literal("Edit Dialog Line"));
        this.parentScreen = parentScreen;
        this.line = line != null ? line.copy() : new DialogLine();
        this.onSave = onSave;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int panelWidth = 460;
        int panelHeight = 310;

        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = (this.height - panelHeight) / 2 + 35;

        int currentY = panelTop + 24;

        // 1. Speaker EditBox
        this.speakerBox = new EditBox(this.font, panelLeft + 20, currentY, panelWidth - 40, 18, Component.literal("Speaker"));
        this.speakerBox.setMaxLength(64);
        this.speakerBox.setValue(line.getSpeaker());
        this.addRenderableWidget(this.speakerBox);
        currentY += 28;

        // 2. Dialog Text EditBox
        this.textBox = new EditBox(this.font, panelLeft + 20, currentY, panelWidth - 40, 36, Component.literal("Dialog Text"));
        this.textBox.setMaxLength(512);
        this.textBox.setValue(line.getText());
        this.addRenderableWidget(this.textBox);
        currentY += 42;

        // 3. Formatting Quick Buttons (&a, &c, &b, &e, &l, &o, &r)
        int toolbarX = panelLeft + 20;
        int btnW = 38;
        int btnH = 16;
        String[] codes = {"&a", "&c", "&b", "&e", "&l", "&o", "&r"};
        for (int i = 0; i < codes.length; i++) {
            final String code = codes[i];
            CyberpunkButton codeBtn = new CyberpunkButton(toolbarX + i * (btnW + 4), currentY, btnW, btnH,
                    DialogFormatUtil.formatText(code + code.substring(1)),
                    b -> insertFormattingCode(code));
            this.addRenderableWidget(codeBtn);
        }
        currentY += 24;

        // 4. Wait for User Input Checkbox
        this.waitForInputCheckbox = new CyberpunkCheckbox(panelLeft + 20, currentY, panelWidth - 40, 18,
                Component.literal("Wait for User Input to Advance (Keypress/Click)"),
                line.isWaitForInput(),
                checked -> {
                    if (delayBox != null) {
                        delayBox.visible = !checked;
                    }
                });
        this.addRenderableWidget(this.waitForInputCheckbox);
        currentY += 24;

        // 5. Line Delay, Char Speed, and Line Sound
        this.delayBox = new EditBox(this.font, panelLeft + 20, currentY, 110, 18, Component.literal("Line Delay (ticks)"));
        this.delayBox.setMaxLength(6);
        this.delayBox.setValue(String.valueOf(line.getDelayTicks()));
        this.delayBox.visible = !line.isWaitForInput();
        this.addRenderableWidget(this.delayBox);

        this.speedBox = new EditBox(this.font, panelLeft + 140, currentY, 120, 18, Component.literal("Char Speed (ticks/char)"));
        this.speedBox.setMaxLength(4);
        this.speedBox.setValue(String.valueOf(line.getCharSpeedTicks()));
        this.addRenderableWidget(this.speedBox);

        this.soundBox = new EditBox(this.font, panelLeft + 270, currentY, panelWidth - 325, 18, Component.literal("Line Sound ID"));
        this.soundBox.setMaxLength(128);
        this.soundBox.setValue(line.getSound());
        this.addRenderableWidget(this.soundBox);

        CyberpunkButton testLineSoundBtn = new CyberpunkButton(panelLeft + panelWidth - 50, currentY, 30, 18, Component.literal("🔊"), b -> {
            String snd = soundBox.getValue().trim();
            if (!snd.isEmpty()) {
                try {
                    SoundEvent soundEvent = ModSounds.resolveSound(snd);
                    if (soundEvent != null) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, line.getPitch()));
                    }
                } catch (Exception ignored) {
                }
            }
        });
        this.addRenderableWidget(testLineSoundBtn);
        currentY += 32;

        int dropdownY = currentY;
        currentY += 24;

        // 6. Per-Letter Sound ID & Min Pitch & Max Pitch & Test Button
        this.letterSoundBox = new EditBox(this.font, panelLeft + 20, currentY, 210, 18, Component.literal("Letter Sound ID"));
        this.letterSoundBox.setMaxLength(128);
        this.letterSoundBox.setValue(line.getLetterSound());
        this.letterSoundBox.setResponder(val -> {
            if (letterSoundDropdown != null) {
                letterSoundDropdown.selectByValue(val.trim());
            }
        });
        this.addRenderableWidget(this.letterSoundBox);

        this.letterPitchMinBox = new EditBox(this.font, panelLeft + 240, currentY, 45, 18, Component.literal("Min Pitch"));
        this.letterPitchMinBox.setMaxLength(6);
        this.letterPitchMinBox.setValue(String.valueOf(line.getLetterSoundPitchMin()));
        this.addRenderableWidget(this.letterPitchMinBox);

        this.letterPitchMaxBox = new EditBox(this.font, panelLeft + 295, currentY, 45, 18, Component.literal("Max Pitch"));
        this.letterPitchMaxBox.setMaxLength(6);
        this.letterPitchMaxBox.setValue(String.valueOf(line.getLetterSoundPitchMax()));
        this.addRenderableWidget(this.letterPitchMaxBox);

        CyberpunkButton testLetterSoundBtn = new CyberpunkButton(panelLeft + panelWidth - 95, currentY, 75, 18, Component.literal("🔊 Test"), b -> {
            String snd = letterSoundBox.getValue().trim();
            if (!snd.isEmpty()) {
                try {
                    float minP = 0.8f;
                    float maxP = 1.2f;
                    try { minP = Float.parseFloat(letterPitchMinBox.getValue().trim()); } catch (Exception ignored) {}
                    try { maxP = Float.parseFloat(letterPitchMaxBox.getValue().trim()); } catch (Exception ignored) {}
                    float pitch = minP + (float) Math.random() * (Math.max(minP, maxP) - Math.min(minP, maxP));
                    pitch = Math.max(0.1f, Math.min(2.0f, pitch));
                    SoundEvent soundEvent = ModSounds.resolveSound(snd);
                    if (soundEvent != null) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch));
                    }
                } catch (Exception ignored) {
                }
            }
        });
        this.addRenderableWidget(testLetterSoundBtn);

        // 7. Undertale Voice Sound Preset Dropdown (Dynamically fetched from resourcepacks & sound registry)
        List<CyberpunkDropdown.DropdownEntry<String>> soundEntries = net.dandare21.fracturedutils.sound.DialogSoundRegistry.getAvailableSoundEntries(line.getLetterSound());

        this.letterSoundDropdown = new CyberpunkDropdown<>(panelLeft + 20, dropdownY, panelWidth - 40, 18, Component.literal("Voice Sound Preset"));
        this.letterSoundDropdown.setOptions(soundEntries);
        this.letterSoundDropdown.setMaxVisibleItems(5);
        this.letterSoundDropdown.selectByValue(line.getLetterSound());
        this.letterSoundDropdown.setOnSelect(entry -> {
            letterSoundBox.setValue(entry.getValue());
        });
        this.addRenderableWidget(this.letterSoundDropdown);

        // 8. Footer Save / Cancel / Custom Camera Setup Buttons
        int btnY = panelTop + panelHeight - 26;

        CyberpunkButton cancelBtn = new CyberpunkButton(panelLeft + 20, btnY, 90, 20, Component.literal("✕ Cancel"), b -> {
            this.minecraft.setScreen(parentScreen);
        }, RED_CANCEL, false);
        this.addRenderableWidget(cancelBtn);

        this.cameraSetupBtn = new CyberpunkButton(panelLeft + 130, btnY, 150, 20, Component.literal("📷 Camera Setup..."), b -> openCameraSetupScreen());
        this.addRenderableWidget(this.cameraSetupBtn);

        CyberpunkButton saveBtn = new CyberpunkButton(panelLeft + panelWidth - 110, btnY, 90, 20, Component.literal("✓ Save"), b -> {
            applyValues();
            if (onSave != null) {
                onSave.accept(line);
            }
            this.minecraft.setScreen(parentScreen);
        });
        this.addRenderableWidget(saveBtn);
    }

    private void openCameraSetupScreen() {
        applyValues();
        this.minecraft.setScreen(new CameraSetupScreen(this, this.line, updatedLine -> {
            this.line.setUseCamera(updatedLine.isUseCamera());
            this.line.setCameraX(updatedLine.getCameraX());
            this.line.setCameraY(updatedLine.getCameraY());
            this.line.setCameraZ(updatedLine.getCameraZ());
            this.line.setCameraYaw(updatedLine.getCameraYaw());
            this.line.setCameraPitch(updatedLine.getCameraPitch());
            this.line.setCameraFov(updatedLine.getCameraFov());
        }));
    }

    private void insertFormattingCode(String code) {
        if (textBox != null && textBox.isFocused()) {
            textBox.insertText(code);
        } else if (speakerBox != null && speakerBox.isFocused()) {
            speakerBox.insertText(code);
        } else if (textBox != null) {
            textBox.insertText(code);
        }
    }

    private void applyValues() {
        line.setSpeaker(speakerBox.getValue());
        line.setText(textBox.getValue());
        line.setWaitForInput(waitForInputCheckbox.isChecked());
        try {
            line.setDelayTicks(Math.max(1, Integer.parseInt(delayBox.getValue().trim())));
        } catch (NumberFormatException e) {
            line.setDelayTicks(40);
        }
        try {
            line.setCharSpeedTicks(Math.max(0, Integer.parseInt(speedBox.getValue().trim())));
        } catch (NumberFormatException e) {
            line.setCharSpeedTicks(1);
        }
        line.setSound(soundBox.getValue().trim());
        line.setLetterSound(letterSoundBox.getValue().trim());
        try {
            line.setLetterSoundPitchMin(Math.max(0.1f, Float.parseFloat(letterPitchMinBox.getValue().trim())));
        } catch (NumberFormatException e) {
            line.setLetterSoundPitchMin(0.8f);
        }
        try {
            line.setLetterSoundPitchMax(Math.max(0.1f, Float.parseFloat(letterPitchMaxBox.getValue().trim())));
        } catch (NumberFormatException e) {
            line.setLetterSoundPitchMax(1.2f);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xBB000000);

        int panelWidth = 460;
        int panelHeight = 310;
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = (this.height - panelHeight) / 2 + 35;

        // --- TOP SECTION: FULL DIALOG PREVIEW BOX ---
        int previewW = 460;
        int previewH = 68;
        int previewX = (this.width - previewW) / 2;
        int previewY = panelTop - previewH - 12;

        guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xEE05090C);
        guiGraphics.fill(previewX, previewY, previewX + previewW, previewY + 1, CYAN_MAIN);
        guiGraphics.fill(previewX, previewY + previewH - 1, previewX + previewW, previewY + previewH, CYAN_MAIN);
        guiGraphics.fill(previewX, previewY, previewX + 1, previewY + previewH, CYAN_MAIN);
        guiGraphics.fill(previewX + previewW - 1, previewY, previewX + previewW, previewY + previewH, CYAN_MAIN);

        guiGraphics.fill(previewX + 2, previewY + 2, previewX + 6, previewY + 3, CYAN_MAIN);
        guiGraphics.fill(previewX + 2, previewY + 2, previewX + 3, previewY + 6, CYAN_MAIN);
        guiGraphics.fill(previewX + previewW - 6, previewY + 2, previewX + previewW - 2, previewY + 3, CYAN_MAIN);
        guiGraphics.fill(previewX + previewW - 3, previewY + 2, previewX + previewW - 2, previewY + 6, CYAN_MAIN);

        int currentTextY = previewY + 8;
        String speakerVal = speakerBox.getValue();
        if (speakerVal != null && !speakerVal.trim().isEmpty()) {
            Component speakerComp = DialogFormatUtil.formatText(speakerVal);
            int spkW = this.font.width(speakerComp);
            int badgeX = previewX + 12;
            int badgeY = previewY - 12;
            int badgeW = spkW + 12;

            guiGraphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 14, 0xEE0A1622);
            guiGraphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 1, CYAN_MAIN);
            guiGraphics.fill(badgeX, badgeY, badgeX + 1, badgeY + 14, CYAN_MAIN);
            guiGraphics.fill(badgeX + badgeW - 1, badgeY, badgeX + badgeW, badgeY + 14, CYAN_MAIN);

            guiGraphics.drawString(this.font, speakerComp, badgeX + 6, badgeY + 3, 0xFFFFFFFF);
            currentTextY += 4;
        }

        Component textComp = DialogFormatUtil.formatText(textBox.getValue());
        List<FormattedCharSequence> wrappedLines = this.font.split(textComp, previewW - 32);
        for (int i = 0; i < Math.min(3, wrappedLines.size()); i++) {
            guiGraphics.drawString(this.font, wrappedLines.get(i), previewX + 16, currentTextY, 0xFFFFFFFF);
            currentTextY += 13;
        }

        // --- BOTTOM SECTION: CONFIG EDITOR PANEL ---
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, CYAN_BG);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 2, CYAN_MAIN);
        guiGraphics.fill(panelLeft, panelTop + panelHeight - 2, panelLeft + panelWidth, panelTop + panelHeight, CYAN_MAIN);
        guiGraphics.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, CYAN_MAIN);
        guiGraphics.fill(panelLeft + panelWidth - 2, panelTop, panelLeft + panelWidth, panelTop + panelHeight, CYAN_MAIN);

        guiGraphics.drawString(this.font, Component.literal("EDIT DIALOG LINE").withStyle(net.minecraft.ChatFormatting.BOLD), panelLeft + 20, panelTop + 8, CYAN_MAIN);
        guiGraphics.fill(panelLeft + 20, panelTop + 20, panelLeft + panelWidth - 20, panelTop + 21, 0xAA00E5FF);

        guiGraphics.drawString(this.font, Component.literal("Speaker (optional):"), panelLeft + 20, panelTop + 14, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Dialog Text (supports &a, &c, &l, etc.):"), panelLeft + 20, panelTop + 42, 0xAAAAAA);

        if (!waitForInputCheckbox.isChecked()) {
            guiGraphics.drawString(this.font, Component.literal("Line Delay (ticks):"), panelLeft + 20, panelTop + 140, 0xAAAAAA);
        }
        guiGraphics.drawString(this.font, Component.literal("Char Speed (ticks/char):"), panelLeft + 140, panelTop + 140, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Line Sound:"), panelLeft + 270, panelTop + 140, 0xAAAAAA);

        guiGraphics.drawString(this.font, Component.literal("Undertale Voice Sound Preset / Custom Sound ID:"), panelLeft + 20, panelTop + 172, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Min P:"), panelLeft + 240, panelTop + 196, 0xAAAAAA);
        guiGraphics.drawString(this.font, Component.literal("Max P:"), panelLeft + 295, panelTop + 196, 0xAAAAAA);

        // Custom Camera Status Info Indicator
        if (line.isUseCamera()) {
            String camText = String.format(java.util.Locale.US, "📷 Cam: Enabled (X:%.1f Y:%.1f Z:%.1f FOV:%.0f°)",
                    line.getCameraX(), line.getCameraY(), line.getCameraZ(), line.getCameraFov());
            guiGraphics.drawString(this.font, Component.literal(camText), panelLeft + 20, panelTop + panelHeight - 44, 0xFF00E5FF);
        } else {
            guiGraphics.drawString(this.font, Component.literal("📷 Cam: Disabled (Player Eye View)"), panelLeft + 20, panelTop + panelHeight - 44, 0xAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (letterSoundDropdown != null) {
            letterSoundDropdown.renderOverlay(guiGraphics, mouseX, mouseY);
        }
    }
}
