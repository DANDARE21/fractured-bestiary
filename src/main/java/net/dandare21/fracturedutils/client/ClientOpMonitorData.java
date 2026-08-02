package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.network.packet.S2CSyncSequenceTelemetryPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientOpMonitorData {
    private static boolean userEnabled = true;
    private static List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> telemetryList = new ArrayList<>();

    private static int hudX = -1;
    private static int hudY = -1;
    private static float hudOpacity = 0.6f;

    public static boolean isEnabled() {
        return userEnabled;
    }

    public static void setEnabled(boolean value) {
        userEnabled = value;
    }

    public static boolean toggleEnabled() {
        userEnabled = !userEnabled;
        return userEnabled;
    }

    public static int getHudX() {
        return hudX;
    }

    public static void setHudX(int x) {
        hudX = x;
    }

    public static int getHudY() {
        return hudY;
    }

    public static void setHudY(int y) {
        hudY = y;
    }

    public static float getHudOpacity() {
        return hudOpacity;
    }

    public static void setHudOpacity(float opacity) {
        hudOpacity = Math.max(0.1f, Math.min(1.0f, opacity));
    }

    public static void resetHudSettings() {
        hudX = -1;
        hudY = -1;
        hudOpacity = 0.6f;
    }

    public static void updateTelemetry(List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> list) {
        telemetryList.clear();
        if (list != null) {
            telemetryList.addAll(list);
        }
    }

    public static void renderHudOverlay(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasPermissions(2)) {
            return;
        }

        Map<String, String> opActions = ClientOperatorActionHandler.getActiveOperatorActions();
        boolean hasPendingOpActions = opActions != null && !opActions.isEmpty();

        // If user disabled OP Monitor HUD and no pending operator action, stay hidden per player!
        if (!userEnabled && !hasPendingOpActions) {
            return;
        }

        // If no telemetry and no pending op actions, stay hidden
        if (telemetryList.isEmpty() && !hasPendingOpActions) {
            return;
        }

        // Don't obscure full screen GUIs (except OrchestratorScreen & OpMonitorConfigScreen)
        if (mc.screen != null && !(mc.screen instanceof net.dandare21.fracturedutils.client.gui.OrchestratorScreen) && !(mc.screen instanceof net.dandare21.fracturedutils.client.gui.OpMonitorConfigScreen)) {
            return;
        }

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int panelW = 260;
        int startX = (hudX >= 0) ? Math.min(screenW - panelW, Math.max(0, hudX)) : (screenW - panelW - 10);
        int currentY = (hudY >= 0) ? Math.min(screenH - 40, Math.max(0, hudY)) : 10;

        int alpha = (int) (hudOpacity * 255) & 0xFF;
        int bgFill = (alpha << 24) | 0x05090C;

        // Render Active Sequences Telemetry Panels
        for (S2CSyncSequenceTelemetryPacket.SequenceTelemetryData seq : telemetryList) {
            List<S2CSyncSequenceTelemetryPacket.ActionInfo> actions = seq.getActions();
            int curIdx = seq.getCurrentIndex();

            int lineH = 14;
            int headerH = 20;

            int startActionIdx = Math.max(0, curIdx - 2);
            int endActionIdx = Math.min(actions.size(), startActionIdx + 4);
            int renderCount = Math.max(0, endActionIdx - startActionIdx);

            int opBoxH = hasPendingOpActions ? 34 : 0;
            int panelH = headerH + renderCount * lineH + opBoxH + 4;

            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);

            int borderColor = hasPendingOpActions ? 0xFFFFD700 : 0xAA00E5FF;
            int accentColor = hasPendingOpActions ? 0xFFFFD700 : 0xFF00E5FF;

            // Container Background & Borders
            graphics.fill(startX, currentY, startX + panelW, currentY + panelH, bgFill);
            graphics.fill(startX, currentY, startX + panelW, currentY + 1, borderColor);
            graphics.fill(startX, currentY + panelH - 1, startX + panelW, currentY + panelH, borderColor);
            graphics.fill(startX, currentY, startX + 1, currentY + panelH, borderColor);
            graphics.fill(startX + panelW - 1, currentY, startX + panelW, currentY + panelH, borderColor);

            // Notch Corner Accents
            graphics.fill(startX, currentY, startX + 4, currentY + 2, accentColor);
            graphics.fill(startX, currentY, startX + 2, currentY + 4, accentColor);
            graphics.fill(startX + panelW - 4, currentY, startX + panelW, currentY + 2, accentColor);
            graphics.fill(startX + panelW - 2, currentY, startX + panelW, currentY + 4, accentColor);

            // Header Title Bar
            graphics.fill(startX + 1, currentY + 1, startX + panelW - 1, currentY + 16, 0xBB081622);

            String stateTag = seq.getState();
            int stateColor = seq.getState().equalsIgnoreCase("RUNNING") ? 0xFF00FF55 : 0xFFFFAA00;
            int stateWidth = font.width(stateTag);

            String rawTitle = "OP MONITOR: " + seq.getSequenceName().toUpperCase();
            int maxTitleW = panelW - stateWidth - 16;
            String titleText = rawTitle;
            if (font.width(rawTitle) > maxTitleW) {
                titleText = font.plainSubstrByWidth(rawTitle, Math.max(10, maxTitleW - font.width(".."))) + "..";
            }

            graphics.drawString(font, Component.literal(titleText).withStyle(ChatFormatting.BOLD), startX + 6, currentY + 4, accentColor, false);
            graphics.drawString(font, stateTag, startX + panelW - stateWidth - 6, currentY + 4, stateColor, false);

            int itemY = currentY + 18;

            // Action Items Breakdown
            for (int i = startActionIdx; i < endActionIdx; i++) {
                S2CSyncSequenceTelemetryPacket.ActionInfo action = actions.get(i);
                boolean isCompleted = i < curIdx;
                boolean isCurrent = i == curIdx;

                String icon = isCompleted ? "✔" : (isCurrent ? "▶" : "⏳");
                int textColor = isCompleted ? 0xAA88AA88 : (isCurrent ? 0xFFFFFFFF : 0xAA8899AA);

                if (isCurrent) {
                    graphics.fill(startX + 4, itemY - 1, startX + panelW - 4, itemY + 12, 0xEE082535);
                    graphics.fill(startX + 4, itemY - 1, startX + 6, itemY + 12, accentColor);
                }

                String actionLine = icon + " [" + (i + 1) + "] " + action.getType().toUpperCase();
                graphics.drawString(font, actionLine, startX + 8, itemY + 1, textColor, false);

                String statusTag = isCompleted ? "DONE" : (isCurrent ? (hasPendingOpActions ? "ACTION REQ" : "RUNNING") : "PENDING");
                int tagColor = isCompleted ? 0xFF55FF55 : (isCurrent ? (hasPendingOpActions ? 0xFFFFD700 : 0xFF00E5FF) : 0xFF667788);
                graphics.drawString(font, statusTag, startX + panelW - font.width(statusTag) - 8, itemY + 1, tagColor, false);

                itemY += lineH;
            }

            // Integrated Operator Action Callout Box inside Sequence Panel
            if (hasPendingOpActions) {
                Map.Entry<String, String> currentEntry = opActions.entrySet().iterator().next();
                String triggerId = currentEntry.getKey();
                String label = currentEntry.getValue();
                String promptText = (label != null && !label.isEmpty()) ? label : ("Trigger: " + triggerId);

                int opBoxY = itemY + 2;
                graphics.fill(startX + 4, opBoxY, startX + panelW - 4, opBoxY + 30, 0xEE1A1208);
                graphics.fill(startX + 4, opBoxY, startX + panelW - 4, opBoxY + 1, 0xFFFFD700);
                graphics.fill(startX + 4, opBoxY + 29, startX + panelW - 4, opBoxY + 30, 0xFFFFD700);

                String keyName = ModKeyBindings.OPERATOR_RESUME_KEY.getTranslatedKeyMessage().getString().toUpperCase();
                String reqTitle = "⚠ ACTION REQUIRED: " + promptText;
                int maxReqW = panelW - 16;
                if (font.width(reqTitle) > maxReqW) {
                    reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(10, maxReqW - font.width(".."))) + "..";
                }
                graphics.drawString(font, Component.literal(reqTitle).withStyle(ChatFormatting.BOLD), startX + 8, opBoxY + 4, 0xFFFFD700, false);

                String holdMsg = "HOLD [" + keyName + "] TO RESUME";
                graphics.drawString(font, Component.literal(holdMsg).withStyle(ChatFormatting.YELLOW), startX + 8, opBoxY + 15, 0xFF00E5FF, false);

                // Hold Progress Bar
                int barX = startX + 8;
                int barY = opBoxY + 25;
                int barW = panelW - 24;
                int barH = 2;

                graphics.fill(barX, barY, barX + barW, barY + barH, 0x77050B10);

                float progress = Math.min(1.0f, ClientOperatorActionHandler.getHoldTicks() / 20.0f);
                int filledW = (int) (barW * progress);
                if (filledW > 0) {
                    int progressColor = progress >= 0.95f ? 0xFF00FF55 : 0xFFFFD700;
                    graphics.fill(barX, barY, barX + filledW, barY + barH, progressColor);
                }
            }

            graphics.pose().popPose();
            currentY += panelH + 6;
        }

        // Fallback Card: Render standalone Operator Action requirement if no sequence telemetry present
        if (telemetryList.isEmpty() && hasPendingOpActions) {
            Map.Entry<String, String> currentEntry = opActions.entrySet().iterator().next();
            String triggerId = currentEntry.getKey();
            String label = currentEntry.getValue();
            String promptText = (label != null && !label.isEmpty()) ? label : ("Trigger: " + triggerId);

            int cardH = 44;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);

            int borderColor = 0xFFFFD700;

            graphics.fill(startX, currentY, startX + panelW, currentY + cardH, bgFill);
            graphics.fill(startX, currentY, startX + panelW, currentY + 1, borderColor);
            graphics.fill(startX, currentY + cardH - 1, startX + panelW, currentY + cardH, borderColor);
            graphics.fill(startX, currentY, startX + 1, currentY + cardH, borderColor);
            graphics.fill(startX + panelW - 1, currentY, startX + panelW, currentY + cardH, borderColor);

            String keyName = ModKeyBindings.OPERATOR_RESUME_KEY.getTranslatedKeyMessage().getString().toUpperCase();
            graphics.drawString(font, Component.literal("⚠ OPERATOR ACTION REQUIRED").withStyle(ChatFormatting.BOLD), startX + 8, currentY + 5, borderColor, false);

            String reqTitle = promptText;
            int maxReqW = panelW - 16;
            if (font.width(reqTitle) > maxReqW) {
                reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(10, maxReqW - font.width(".."))) + "..";
            }
            graphics.drawString(font, Component.literal(reqTitle), startX + 8, currentY + 17, 0xFFFFFFFF, false);

            String holdMsg = "HOLD [" + keyName + "] TO RESUME";
            graphics.drawString(font, Component.literal(holdMsg).withStyle(ChatFormatting.YELLOW), startX + 8, currentY + 28, 0xFF00E5FF, false);

            int barX = startX + 8;
            int barY = currentY + cardH - 3;
            int barW = panelW - 16;
            int barH = 2;

            graphics.fill(barX, barY, barX + barW, barY + barH, 0x77050B10);

            float progress = Math.min(1.0f, ClientOperatorActionHandler.getHoldTicks() / 20.0f);
            int filledW = (int) (barW * progress);
            if (filledW > 0) {
                int progressColor = progress >= 0.95f ? 0xFF00FF55 : 0xFFFFD700;
                graphics.fill(barX, barY, barX + filledW, barY + barH, progressColor);
            }

            graphics.pose().popPose();
        }
    }
}
