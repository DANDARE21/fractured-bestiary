package net.dandare21.fracturedutils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class RenameSequenceModalScreen extends Screen {
    private static final int CYAN_MAIN = 0xFF00E5FF;
    private static final int CYAN_BG = 0xFF05090C;
    private static final int RED_CANCEL = 0xFFFF3355;

    private final Screen parentScreen;
    private final String oldFileName;
    private final Consumer<String> onRename;
    private EditBox inputField;

    public RenameSequenceModalScreen(Screen parentScreen, String oldFileName, Consumer<String> onRename) {
        super(Component.literal("Rename Sequence"));
        this.parentScreen = parentScreen;
        this.oldFileName = oldFileName != null ? oldFileName : "sequence.json";
        this.onRename = onRename;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int panelWidth = 320;
        int panelHeight = 150;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        int fieldY = top + 55;
        this.inputField = new EditBox(this.font, left + 25, fieldY, panelWidth - 50, 20, Component.literal("Sequence Name"));
        this.inputField.setMaxLength(128);
        this.inputField.setBordered(false);
        this.inputField.setTextColor(0xFFFFFFFF);
        this.inputField.setValue(oldFileName);
        this.addRenderableWidget(this.inputField);

        int bottomY = top + panelHeight - 34;
        this.addRenderableWidget(new CyberpunkButton(left + panelWidth / 2 - 95, bottomY, 90, 24, Component.literal("RENAME"), b -> {
            String newName = inputField.getValue().trim();
            if (!newName.endsWith(".json")) newName += ".json";
            if (!newName.isEmpty() && onRename != null) {
                onRename.accept(newName);
            }
            if (this.minecraft != null) {
                this.minecraft.setScreen(parentScreen);
            }
        }, CYAN_MAIN, false));

        this.addRenderableWidget(new CyberpunkButton(left + panelWidth / 2 + 5, bottomY, 90, 24, Component.literal("CANCEL"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parentScreen);
            }
        }, RED_CANCEL, false));
    }

    private void drawGridOverlay(GuiGraphics graphics) {
        int gridSize = 32;
        long time = System.currentTimeMillis();
        int offsetX = (int) ((time / 40) % gridSize);
        int offsetY = (int) ((time / 40) % gridSize);

        for (int x = -gridSize + offsetX; x < this.width + gridSize; x += gridSize) {
            graphics.fill(x, 0, x + 1, this.height, 0x1200E5FF);
        }
        for (int y = -gridSize + offsetY; y < this.height + gridSize; y += gridSize) {
            graphics.fill(0, y, this.width, y + 1, 0x1200E5FF);
        }
    }

    private void drawBorderBox(GuiGraphics graphics, int x, int y, int w, int h, int borderColor, int fillColor) {
        graphics.fill(x, y, x + w, y + h, fillColor);
        graphics.fill(x, y, x + w, y + 1, borderColor);
        graphics.fill(x, y + h - 1, x + w, y + h, borderColor);
        graphics.fill(x, y, x + 1, y + h, borderColor);
        graphics.fill(x + w - 1, y, x + w, y + h, borderColor);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, CYAN_BG);
        drawGridOverlay(graphics);

        int panelWidth = 320;
        int panelHeight = 150;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        drawBorderBox(graphics, left, top, panelWidth, panelHeight, CYAN_MAIN, 0xEE060C12);

        // Header Title Bar
        graphics.fill(left, top, left + panelWidth, top + 26, 0xEE081622);
        graphics.fill(left, top + 25, left + panelWidth, top + 26, CYAN_MAIN);
        graphics.drawString(this.font, "RENAME SEQUENCE FILE", left + 12, top + 8, CYAN_MAIN, false);

        graphics.drawString(this.font, "New Sequence File Name:", left + 20, top + 38, 0xFFAABBCC, false);
        drawBorderBox(graphics, left + 20, top + 53, panelWidth - 40, 24, 0xAA00E5FF, 0xEE08121B);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
