package net.dandare21.fracturedutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CyberpunkSlider extends AbstractSliderButton {
    private final String prefix;
    private final Consumer<Double> onChange;

    public CyberpunkSlider(int x, int y, int width, int height, String prefix, double initialValue, Consumer<Double> onChange) {
        super(x, y, width, height, Component.literal(prefix + ": " + (int) (initialValue * 100) + "%"), initialValue);
        this.prefix = prefix;
        this.onChange = onChange;
        updateMessage();
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = Math.max(0.0, Math.min(1.0, value));
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(prefix + ": " + (int) (this.value * 100) + "%"));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(this.value);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int borderColor = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF00E5FF;
        int bg = this.isHoveredOrFocused() ? 0xEE082535 : 0xEE060C12;

        // Track fill & border
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        graphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
        graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        graphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

        // Thumb position
        int thumbW = 10;
        int thumbX = this.getX() + (int) (this.value * (this.width - thumbW));
        int thumbBg = 0xFF00E5FF;

        graphics.fill(thumbX, this.getY() + 2, thumbX + thumbW, this.getY() + this.height - 2, thumbBg);
        graphics.fill(thumbX, this.getY() + 2, thumbX + thumbW, this.getY() + 3, 0xFFFFFFFF);

        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFFFF);
    }
}
