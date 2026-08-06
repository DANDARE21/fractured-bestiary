package net.dandare21.fracturedutils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.ping.HudPing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientPingRenderer {

    private static final ResourceLocation PING_TEXTURE = new ResourceLocation(FracturedUtils.MOD_ID, "textures/gui/ping_marker.png");

    public record RenderedPingInfo(
            HudPing ping,
            double screenX,
            double screenY,
            double distance,
            boolean isOffScreen,
            double angleRad
    ) {}

    private static final Map<String, RenderedPingInfo> cachedPingPositions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cachedPingPositions.clear();
            return;
        }

        List<HudPing> pings = ClientPingData.getActivePings();
        if (pings.isEmpty()) {
            cachedPingPositions.clear();
            return;
        }

        String currentDim = mc.level.dimension().location().toString();
        Vec3 camPos = event.getCamera().getPosition();

        // Prepare matrices for exact 3D to 2D screen projection
        Matrix4f modelViewMatrix = new Matrix4f(event.getPoseStack().last().pose());
        modelViewMatrix.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
        Matrix4f projMatrix = new Matrix4f(event.getProjectionMatrix());

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double centerX = screenWidth / 2.0;
        double centerY = screenHeight / 2.0;

        List<RenderedPingInfo> currentFrameInfos = new ArrayList<>();

        for (HudPing ping : pings) {
            if (!currentDim.equals(ping.getDimension())) {
                continue;
            }

            double px = ping.getX();
            double py = ping.getY();
            double pz = ping.getZ();

            double dx = px - camPos.x;
            double dy = py - camPos.y;
            double dz = pz - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Compute exact matrix projection
            Vector4f clipPos = new Vector4f((float) px, (float) py, (float) pz, 1.0f);
            clipPos.mul(modelViewMatrix);
            clipPos.mul(projMatrix);

            double screenX;
            double screenY;
            boolean isOffScreen;
            double angleRad = 0.0;

            float w = clipPos.w();
            if (w > 0.001f) {
                float ndcX = clipPos.x() / w;
                float ndcY = clipPos.y() / w;

                screenX = (1.0 + ndcX) * 0.5 * screenWidth;
                screenY = (1.0 - ndcY) * 0.5 * screenHeight;

                double margin = 32.0;
                if (ndcX < -0.9f || ndcX > 0.9f || ndcY < -0.9f || ndcY > 0.9f ||
                        screenX < margin || screenX > screenWidth - margin ||
                        screenY < margin || screenY > screenHeight - margin) {
                    isOffScreen = true;
                    double dirX = screenX - centerX;
                    double dirY = screenY - centerY;
                    angleRad = Math.atan2(dirY, dirX);
                    Vec2 clamped = clampToEdge(dirX, dirY, centerX, centerY, margin);
                    screenX = clamped.x;
                    screenY = clamped.y;
                } else {
                    isOffScreen = false;
                }
            } else {
                // Behind camera: invert clip coordinates
                isOffScreen = true;
                float ndcX = -clipPos.x();
                float ndcY = clipPos.y();
                angleRad = Math.atan2(-ndcY, -ndcX);
                Vec2 clamped = clampToEdge(-ndcX, -ndcY, centerX, centerY, 32.0);
                screenX = clamped.x;
                screenY = clamped.y;
            }

            currentFrameInfos.add(new RenderedPingInfo(ping, screenX, screenY, dist, isOffScreen, angleRad));
        }

        // Update cached position map for GUI overlay
        cachedPingPositions.clear();
        for (RenderedPingInfo info : currentFrameInfos) {
            cachedPingPositions.put(info.ping.getId(), info);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }

        if (cachedPingPositions.isEmpty()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();

        for (RenderedPingInfo info : cachedPingPositions.values()) {
            renderHudPing(guiGraphics, mc, info);
        }
    }

    private static void renderHudPing(GuiGraphics guiGraphics, Minecraft mc, RenderedPingInfo info) {
        HudPing ping = info.ping;
        int sx = (int) info.screenX;
        int sy = (int) info.screenY;
        double dist = info.distance;
        boolean isOffScreen = info.isOffScreen;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int color = ping.getColor();
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a <= 0.0f) a = 1.0f;

        int iconSize = isOffScreen ? 24 : 28;
        int halfSize = iconSize / 2;

        // Render full 64x64 sprite scaled to iconSize x iconSize with color tint
        RenderSystem.setShaderColor(r, g, b, a);
        guiGraphics.blit(PING_TEXTURE, sx - halfSize, sy - halfSize, iconSize, iconSize, 0.0f, 0.0f, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Distance text
        String distStr = dist >= 1000.0 ? String.format("%.1fkm", dist / 1000.0) : String.format("%.0fm", dist);
        int textW = mc.font.width(distStr);
        int textX = sx - textW / 2;
        int textY = sy + halfSize + 3;

        int fillX1 = textX - 3;
        int fillY1 = textY - 1;
        int fillX2 = textX + textW + 3;
        int fillY2 = textY + 9;

        // Background pill
        guiGraphics.fill(fillX1, fillY1, fillX2, fillY2, 0xCC08121B);
        guiGraphics.fill(fillX1, fillY1, fillX2, fillY1 + 1, color);

        guiGraphics.drawString(mc.font, distStr, textX, textY, 0xFFFFFFFF, true);

        // Render Label if defined
        if (ping.getLabel() != null && !ping.getLabel().isEmpty() && !ping.getLabel().equalsIgnoreCase(ping.getId())) {
            String labelStr = ping.getLabel();
            int labelW = mc.font.width(labelStr);
            int labelX = sx - labelW / 2;
            int labelY = sy - halfSize - 11;

            guiGraphics.fill(labelX - 3, labelY - 1, labelX + labelW + 3, labelY + 9, 0xCC08121B);
            guiGraphics.drawString(mc.font, labelStr, labelX, labelY, color, true);
        }

        RenderSystem.disableBlend();
    }

    private static Vec2 clampToEdge(double dx, double dy, double centerX, double centerY, double margin) {
        double minX = margin;
        double maxX = centerX * 2.0 - margin;
        double minY = margin;
        double maxY = centerY * 2.0 - margin;

        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
            return new Vec2(centerX, maxY);
        }

        double scaleX = dx > 0 ? (maxX - centerX) / dx : (minX - centerX) / dx;
        double scaleY = dy > 0 ? (maxY - centerY) / dy : (minY - centerY) / dy;
        double scale = Math.min(Math.abs(scaleX), Math.abs(scaleY));

        double clampedX = centerX + dx * scale;
        double clampedY = centerY + dy * scale;

        clampedX = Math.max(minX, Math.min(maxX, clampedX));
        clampedY = Math.max(minY, Math.min(maxY, clampedY));

        return new Vec2(clampedX, clampedY);
    }

    private record Vec2(double x, double y) {}
}
