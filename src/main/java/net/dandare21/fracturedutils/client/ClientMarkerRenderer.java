package net.dandare21.fracturedutils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FracturedUtils.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientMarkerRenderer {

    public record MarkerAreaInfo(double x, double y, double z, double radius, boolean opsOnly) {}

    private static final ResourceLocation GRADIENT_TEXTURE = new ResourceLocation(FracturedUtils.MOD_ID, "textures/misc/radius_gradient.png");
    private static final List<MarkerAreaInfo> activeMarkers = new ArrayList<>();

    public static synchronized void setActiveMarkers(List<MarkerAreaInfo> markers) {
        activeMarkers.clear();
        if (markers != null) {
            activeMarkers.addAll(markers);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<MarkerAreaInfo> markersToRender;
        synchronized (ClientMarkerRenderer.class) {
            if (activeMarkers.isEmpty()) return;
            markersToRender = new ArrayList<>(activeMarkers);
        }

        boolean isOp = mc.player.hasPermissions(2);
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, GRADIENT_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        for (MarkerAreaInfo marker : markersToRender) {
            if (marker.opsOnly() && !isOp) {
                continue;
            }
            renderGradientCylinder(poseStack, buffer, tesselator, marker.x(), marker.y(), marker.z(), marker.radius(), 3.0);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderGradientCylinder(PoseStack poseStack, BufferBuilder buffer, Tesselator tesselator, double cx, double cy, double cz, double radius, double height) {
        int segments = 48;
        double minY = cy;
        double maxY = cy + height;

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        PoseStack.Pose lastPose = poseStack.last();

        for (int i = 0; i <= segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            float vx = (float) (cx + dx);
            float vz = (float) (cz + dz);
            float u = (float) i / segments;

            // Top vertex
            buffer.vertex(lastPose.pose(), vx, (float) maxY, vz)
                    .uv(u, 0.0f)
                    .color(0, 229, 255, 220)
                    .endVertex();

            // Bottom vertex
            buffer.vertex(lastPose.pose(), vx, (float) minY, vz)
                    .uv(u, 1.0f)
                    .color(0, 229, 255, 220)
                    .endVertex();
        }

        tesselator.end();
    }
}
