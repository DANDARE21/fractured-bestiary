package net.dandare21.fracturedutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CyberpunkCheckbox extends AbstractWidget {
    private boolean checked;
    private final int accentColor;
    private final Consumer<Boolean> onToggle;

    public CyberpunkCheckbox(int x, int y, int width, int height, Component message, boolean initialValue) {
        this(x, y, width, height, message, initialValue, 0xFF00E5FF, null);
    }

    public CyberpunkCheckbox(int x, int y, int width, int height, Component message, boolean initialValue, Consumer<Boolean> onToggle) {
        this(x, y, width, height, message, initialValue, 0xFF00E5FF, onToggle);
    }

    public CyberpunkCheckbox(int x, int y, int width, int height, Component message, boolean initialValue, int accentColor, Consumer<Boolean> onToggle) {
        super(x, y, width, height, message);
        this.checked = initialValue;
        this.accentColor = accentColor;
        this.onToggle = onToggle;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void toggle() {
        this.checked = !this.checked;
        if (onToggle != null) {
            onToggle.accept(this.checked);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        toggle();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean isHovered = this.active && this.isHoveredOrFocused();

        int boxSize = 14;
        int boxX = this.getX();
        int boxY = this.getY() + (this.height - boxSize) / 2;

        int borderColor = !this.active ? 0x44445566 : (isHovered ? 0xFFFFFFFF : accentColor);
        int fillColor = !this.active ? 0xEE0A0F14 : (isHovered ? 0xEE082535 : 0xEE081622);
        int textColor = !this.active ? 0xFF556677 : (isHovered ? 0xFFFFFFFF : 0xFFAABBCC);

        // Render Checkbox Box Frame
        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, fillColor);
        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + 1, borderColor);
        guiGraphics.fill(boxX, boxY + boxSize - 1, boxX + boxSize, boxY + boxSize, borderColor);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxSize, borderColor);
        guiGraphics.fill(boxX + boxSize - 1, boxY, boxX + boxSize, boxY + boxSize, borderColor);

        // Inner Check Mark / Glowing Fill if Checked
        if (this.checked) {
            guiGraphics.fill(boxX + 3, boxY + 3, boxX + boxSize - 3, boxY + boxSize - 3, accentColor);
            Font font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, "✔", boxX + 3, boxY + 3, 0xFF00FF55, false);
        }

        // Render Label Text alongside Box
        Font font = Minecraft.getInstance().font;
        int textX = boxX + boxSize + 8;
        int textY = this.getY() + (this.height - 8) / 2;
        guiGraphics.drawString(font, this.getMessage(), textX, textY, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
