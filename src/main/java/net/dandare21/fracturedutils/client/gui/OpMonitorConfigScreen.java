package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.client.ClientConfig;
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

    private static double scaleToSlider(float scale) {
        return Math.max(0.0, Math.min(1.0, (scale - 0.5f) / 1.5f));
    }

    private static float sliderToScale(double sliderValue) {
        return Math.max(0.5f, Math.min(2.0f, 0.5f + (float) sliderValue * 1.5f));
    }

    @Override
    protected void init() {
        super.init();

        int center = this.width / 2;
        int sliderY = this.height - 52;
        int buttonY = this.height - 26;

        // Opacity Slider Widget
        CyberpunkSlider opacitySlider = new CyberpunkSlider(center - 155, sliderY, 150, 20, "HUD Opacity",
                ClientOpMonitorData.getHudOpacity(), val -> {
                    ClientOpMonitorData.setHudOpacity(val.floatValue());
                });
        this.addRenderableWidget(opacitySlider);

        // Size Slider Widget
        CyberpunkSlider sizeSlider = new CyberpunkSlider(center + 5, sliderY, 150, 20, "HUD Size",
                scaleToSlider(ClientOpMonitorData.getHudScale()), val -> {
                    ClientOpMonitorData.setHudScale(sliderToScale(val));
                }, val -> "HUD Size: " + (int) (sliderToScale(val) * 100) + "%");
        this.addRenderableWidget(sizeSlider);

        // Reset Button
        this.addRenderableWidget(new CyberpunkButton(center - 80, buttonY, 75, 20, Component.literal("RESET"), b -> {
            ClientOpMonitorData.resetHudSettings();
            opacitySlider.setValue(ClientOpMonitorData.getHudOpacity());
            sizeSlider.setValue(scaleToSlider(ClientOpMonitorData.getHudScale()));
        }, CYAN_MAIN, false, Component.literal("Reset position, size and opacity to defaults")));

        // Done / Save Button
        this.addRenderableWidget(new CyberpunkButton(center + 5, buttonY, 75, 20, Component.literal("DONE"), b -> {
            ClientConfig.save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(parentScreen);
            }
        }, 0xFF55FF55, false, Component.literal("Save settings and return")));
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        super.onClose();
    }

    private int getHudWidth() {
        return (int) (260 * ClientOpMonitorData.getHudScale());
    }

    private int getHudHeight() {
        return (int) (68 * ClientOpMonitorData.getHudScale());
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
            int hW = getHudWidth();
            int hH = getHudHeight();
            int newX = Math.max(0, Math.min(this.width - hW, (int) (mouseX - dragOffsetX)));
            int newY = Math.max(0, Math.min(this.height - hH, (int) (mouseY - dragOffsetY)));
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
            ClientConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darkened background overlay
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        // Header Instructions
        graphics.drawCenteredString(this.font,
                Component.literal("OP MONITOR HUD CONFIGURATION").withStyle(ChatFormatting.BOLD), this.width / 2, 16,
                CYAN_MAIN);
        graphics.drawCenteredString(this.font,
                Component.literal("Click & Drag the HUD overlay to reposition  |  Adjust opacity & size below"),
                this.width / 2, 30, 0xFFAABBCC);

        // Render real HUD or preview box
        int hX = getHudX();
        int hY = getHudY();
        int hW = getHudWidth();
        int hH = getHudHeight();

        float scale = ClientOpMonitorData.getHudScale();
        int unscaledW = 260;
        int unscaledH = 68;

        // Render HUD Preview Box with glowing borders
        int alpha = (int) (ClientOpMonitorData.getHudOpacity() * 255) & 0xFF;
        int bgFill = (alpha << 24) | 0x05090C;
        int borderCol = isDragging ? 0xFF00FFFF : CYAN_MAIN;

        graphics.pose().pushPose();
        graphics.pose().translate(hX, hY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.fill(0, 0, unscaledW, unscaledH, bgFill);
        graphics.fill(0, 0, unscaledW, 1, borderCol);
        graphics.fill(0, unscaledH - 1, unscaledW, unscaledH, borderCol);
        graphics.fill(0, 0, 1, unscaledH, borderCol);
        graphics.fill(unscaledW - 1, 0, unscaledW, unscaledH, borderCol);

        // Inner header tag
        graphics.fill(1, 1, unscaledW - 1, 16, 0xBB081622);
        graphics.drawString(this.font, Component.literal("≡ OP MONITOR PREVIEW").withStyle(ChatFormatting.BOLD), 6, 4,
                borderCol, false);

        graphics.drawString(this.font, "✔ [1] /tp %player% 100 64 200", 8, 20, 0xAA88AA88, false);
        graphics.drawString(this.font, "DONE", unscaledW - 38, 20, 0xFF55FF55, false);

        graphics.fill(4, 33, unscaledW - 4, 46, 0xEE082535);
        graphics.drawString(this.font, "▶ [2] Op Action: Press Button", 8, 35, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "RUNNING", unscaledW - 55, 35, CYAN_MAIN, false);

        graphics.drawString(this.font, "⏳ [3] Run Seq: outro.json", 8, 49, 0xAA8899AA, false);
        graphics.drawString(this.font, "PENDING", unscaledW - 52, 49, 0xFF667788, false);

        graphics.pose().popPose();

        // Hover tooltip on preview box
        if (mouseX >= hX && mouseX <= hX + hW && mouseY >= hY && mouseY <= hY + hH) {
            graphics.renderTooltip(this.font, Component.literal("Click & Drag to move HUD"), mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
