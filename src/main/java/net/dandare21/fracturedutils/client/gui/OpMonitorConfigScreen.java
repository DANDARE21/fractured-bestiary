package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.client.ClientOpMonitorData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OpMonitorConfigScreen extends Screen {
    private static final int CYAN_MAIN = 0xFF00E5FF;
    private final Screen parentScreen;

    private boolean isDragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public OpMonitorConfigScreen(Screen parentScreen) {
        super(Component.literal("OP Monitor Configuration"));
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        int bottomY = this.height - 40;
        int center = this.width / 2;

        // Opacity Slider Widget
        CyberpunkSlider slider = new CyberpunkSlider(center - 160, bottomY, 150, 20, "HUD Opacity", ClientOpMonitorData.getHudOpacity(), val -> {
            ClientOpMonitorData.setHudOpacity(val.floatValue());
        });
        this.addRenderableWidget(slider);

        // Reset Button
        this.addRenderableWidget(new CyberpunkButton(center + 0, bottomY, 75, 20, Component.literal("RESET"), b -> {
            ClientOpMonitorData.resetHudSettings();
            slider.setValue(ClientOpMonitorData.getHudOpacity());
        }, CYAN_MAIN, false, Component.literal("Reset position and opacity to defaults")));

        // Done / Save Button
        this.addRenderableWidget(new CyberpunkButton(center + 80, bottomY, 75, 20, Component.literal("DONE"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(parentScreen);
            }
        }, 0xFF55FF55, false, Component.literal("Save settings and return")));
    }

    private int getHudWidth() {
        return 260;
    }

    private int getHudHeight() {
        return 68;
    }

    private int getHudX() {
        int panelW = getHudWidth();
        int curX = ClientOpMonitorData.getHudX();
        return (curX >= 0) ? Math.min(this.width - panelW, Math.max(0, curX)) : (this.width - panelW - 10);
    }

    private int getHudY() {
        int curY = ClientOpMonitorData.getHudY();
        return (curY >= 0) ? Math.min(this.height - getHudHeight(), Math.max(0, curY)) : 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int hX = getHudX();
            int hY = getHudY();
            int hW = getHudWidth();
            int hH = getHudHeight();

            if (mouseX >= hX && mouseX <= hX + hW && mouseY >= hY && mouseY <= hY + hH) {
                this.isDragging = true;
                this.dragOffsetX = mouseX - hX;
                this.dragOffsetY = mouseY - hY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            ClientOpMonitorData.setHudX(newX);
            ClientOpMonitorData.setHudY(newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isDragging) {
            this.isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darkened background overlay
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        // Header Instructions
        graphics.drawCenteredString(this.font, Component.literal("OP MONITOR HUD CONFIGURATION").withStyle(ChatFormatting.BOLD), this.width / 2, 16, CYAN_MAIN);
        graphics.drawCenteredString(this.font, Component.literal("Click & Drag the HUD overlay to reposition  |  Adjust opacity below"), this.width / 2, 30, 0xFFAABBCC);

        // Render real HUD or preview box
        int hX = getHudX();
        int hY = getHudY();
        int hW = getHudWidth();
        int hH = getHudHeight();

        // Render HUD Preview Box with glowing borders
        int alpha = (int) (ClientOpMonitorData.getHudOpacity() * 255) & 0xFF;
        int bgFill = (alpha << 24) | 0x05090C;
        int borderCol = isDragging ? 0xFF00FFFF : CYAN_MAIN;

        graphics.fill(hX, hY, hX + hW, hY + hH, bgFill);
        graphics.fill(hX, hY, hX + hW, hY + 1, borderCol);
        graphics.fill(hX, hY + hH - 1, hX + hW, hY + hH, borderCol);
        graphics.fill(hX, hY, hX + 1, hY + hH, borderCol);
        graphics.fill(hX + hW - 1, hY, hX + hW, hY + hH, borderCol);

        // Inner header tag
        graphics.fill(hX + 1, hY + 1, hX + hW - 1, hY + 16, 0xBB081622);
        graphics.drawString(this.font, Component.literal("≡ OP MONITOR PREVIEW (DRAG ME)").withStyle(ChatFormatting.BOLD), hX + 6, hY + 4, borderCol, false);

        graphics.drawString(this.font, "✔ [1] COMMAND ACTION", hX + 8, hY + 20, 0xAA88AA88, false);
        graphics.drawString(this.font, "DONE", hX + hW - 38, hY + 20, 0xFF55FF55, false);

        graphics.fill(hX + 4, hY + 33, hX + hW - 4, hY + 46, 0xEE082535);
        graphics.drawString(this.font, "▶ [2] WAIT UNTIL ACTION", hX + 8, hY + 35, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "RUNNING", hX + hW - 55, hY + 35, CYAN_MAIN, false);

        graphics.drawString(this.font, "⏳ [3] RUN SUBSEQUENCE", hX + 8, hY + 49, 0xAA8899AA, false);
        graphics.drawString(this.font, "PENDING", hX + hW - 52, hY + 49, 0xFF667788, false);

        // Hover tooltip on preview box
        if (mouseX >= hX && mouseX <= hX + hW && mouseY >= hY && mouseY <= hY + hH) {
            graphics.renderTooltip(this.font, Component.literal("Click & Drag to move HUD"), mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
