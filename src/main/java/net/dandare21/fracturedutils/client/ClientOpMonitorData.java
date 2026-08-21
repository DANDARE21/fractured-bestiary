package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.network.packet.S2CSyncSequenceTelemetryPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientOpMonitorData {
    private static List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> telemetryList = new ArrayList<>();

    public static List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> getTelemetryList() {
        return telemetryList;
    }

    public static boolean isEnabled() {
        return ClientConfig.isOpMonitorEnabled();
    }

    public static void setEnabled(boolean value) {
        ClientConfig.setOpMonitorEnabled(value);
    }

    public static boolean toggleEnabled() {
        boolean newState = !isEnabled();
        setEnabled(newState);
        return newState;
    }

    public static int getHudX() {
        return ClientConfig.getOpMonitorX();
    }

    public static void setHudX(int x) {
        ClientConfig.setOpMonitorX(x);
    }

    public static int getHudY() {
        return ClientConfig.getOpMonitorY();
    }

    public static void setHudY(int y) {
        ClientConfig.setOpMonitorY(y);
    }

    public static float getHudOpacity() {
        return ClientConfig.getOpMonitorOpacity();
    }

    public static void setHudOpacity(float opacity) {
        ClientConfig.setOpMonitorOpacity(opacity);
    }

    public static float getHudScale() {
        return ClientConfig.getOpMonitorScale();
    }

    public static void setHudScale(float scale) {
        ClientConfig.setOpMonitorScale(scale);
    }

    public static void resetHudSettings() {
        ClientConfig.resetOpMonitorSettings();
    }

    public static void updateTelemetry(List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> list) {
        telemetryList.clear();
        if (list != null) {
            telemetryList.addAll(list);
        }
        updateActiveMarkerRenderers();
    }

    private static void updateActiveMarkerRenderers() {
        List<ClientMarkerRenderer.MarkerAreaInfo> markers = new ArrayList<>();
        for (S2CSyncSequenceTelemetryPacket.SequenceTelemetryData seq : telemetryList) {
            List<S2CSyncSequenceTelemetryPacket.ActionInfo> actions = seq.getActions();
            int currentIdx = seq.getCurrentIndex();
            if (currentIdx >= 0 && currentIdx < actions.size()) {
                S2CSyncSequenceTelemetryPacket.ActionInfo currentAction = actions.get(currentIdx);
                String details = currentAction.getDetails();
                if (details != null && details.startsWith("proximity:")) {
                    parseAndAddActiveMarker(details, markers);
                }
            }
        }
        ClientMarkerRenderer.setActiveMarkers(markers);
    }

    private static void parseAndAddActiveMarker(String details, List<ClientMarkerRenderer.MarkerAreaInfo> markers) {
        try {
            if (!details.contains("area=true")) {
                return;
            }
            String coordsPart = details.substring("proximity:".length(), details.indexOf(' '));
            String[] c = coordsPart.split(",");
            double x = Double.parseDouble(c[0]);
            double y = Double.parseDouble(c[1]);
            double z = Double.parseDouble(c[2]);

            int rIdx = details.indexOf("r=");
            int rEnd = details.indexOf(' ', rIdx);
            double radius = Double.parseDouble(details.substring(rIdx + 2, rEnd > 0 ? rEnd : details.length()));

            boolean opsOnly = details.contains("Ops");
            markers.add(new ClientMarkerRenderer.MarkerAreaInfo(x, y, z, radius, opsOnly));
        } catch (Exception ignored) {}
    }

    public static void renderHudOverlay(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasPermissions(2)) {
            return;
        }

        Map<String, String> opActions = ClientOperatorActionHandler.getActiveOperatorActions();
        boolean hasPendingOpActions = opActions != null && !opActions.isEmpty();

        // If user disabled OP Monitor HUD and no pending operator action, stay hidden
        // per player!
        if (!isEnabled() && !hasPendingOpActions) {
            return;
        }

        // If no telemetry and no pending op actions, stay hidden
        if (telemetryList.isEmpty() && !hasPendingOpActions) {
            return;
        }

        // Don't obscure full screen GUIs (except OrchestratorScreen &
        // OpMonitorConfigScreen)
        if (mc.screen != null && !(mc.screen instanceof net.dandare21.fracturedutils.client.gui.OrchestratorScreen)
                && !(mc.screen instanceof net.dandare21.fracturedutils.client.gui.OpMonitorConfigScreen)) {
            return;
        }

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        float scale = getHudScale();
        int unscaledPanelW = 260;
        int scaledPanelW = (int) (unscaledPanelW * scale);

        int hudX = getHudX();
        int hudY = getHudY();

        int startX = (hudX >= 0) ? Math.min(screenW - scaledPanelW, Math.max(0, hudX)) : (screenW - scaledPanelW - 10);
        int currentY = (hudY >= 0) ? Math.min(screenH - (int) (40 * scale), Math.max(0, hudY)) : 10;

        int alpha = (int) (getHudOpacity() * 255) & 0xFF;
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
            int unscaledPanelH = headerH + renderCount * lineH + opBoxH + 4;
            int scaledPanelH = (int) (unscaledPanelH * scale);

            graphics.pose().pushPose();
            graphics.pose().translate(startX, currentY, 200);
            graphics.pose().scale(scale, scale, 1.0f);

            int borderColor = hasPendingOpActions ? 0xFFFFD700 : 0xAA00E5FF;
            int accentColor = hasPendingOpActions ? 0xFFFFD700 : 0xFF00E5FF;

            // Container Background & Borders
            graphics.fill(0, 0, unscaledPanelW, unscaledPanelH, bgFill);
            graphics.fill(0, 0, unscaledPanelW, 1, borderColor);
            graphics.fill(0, unscaledPanelH - 1, unscaledPanelW, unscaledPanelH, borderColor);
            graphics.fill(0, 0, 1, unscaledPanelH, borderColor);
            graphics.fill(unscaledPanelW - 1, 0, unscaledPanelW, unscaledPanelH, borderColor);

            // Notch Corner Accents
            graphics.fill(0, 0, 4, 2, accentColor);
            graphics.fill(0, 0, 2, 4, accentColor);
            graphics.fill(unscaledPanelW - 4, 0, unscaledPanelW, 2, accentColor);
            graphics.fill(unscaledPanelW - 2, 0, unscaledPanelW, 4, accentColor);

            // Header Title Bar
            graphics.fill(1, 1, unscaledPanelW - 1, 16, 0xBB081622);

            String stateTag = seq.getState();
            int stateColor = seq.getState().equalsIgnoreCase("RUNNING") ? 0xFF00FF55 : 0xFFFFAA00;
            int stateWidth = font.width(stateTag);

            String rawTitle = seq.getSequenceName().toUpperCase();
            int maxTitleW = unscaledPanelW - stateWidth - 16;
            String titleText = rawTitle;
            if (font.width(rawTitle) > maxTitleW) {
                titleText = font.plainSubstrByWidth(rawTitle, Math.max(10, maxTitleW - font.width(".."))) + "..";
            }

            graphics.drawString(font, Component.literal(titleText).withStyle(ChatFormatting.BOLD), 6, 4, accentColor,
                    false);
            graphics.drawString(font, stateTag, unscaledPanelW - stateWidth - 6, 4, stateColor, false);

            int itemY = 18;

            // Action Items Breakdown
            for (int i = startActionIdx; i < endActionIdx; i++) {
                S2CSyncSequenceTelemetryPacket.ActionInfo action = actions.get(i);
                boolean isCompleted = i < curIdx;
                boolean isCurrent = i == curIdx;

                String icon = isCompleted ? "✔" : (isCurrent ? "▶" : "⏳");
                int textColor = isCompleted ? 0xAA88AA88 : (isCurrent ? 0xFFFFFFFF : 0xAA8899AA);

                if (isCurrent) {
                    graphics.fill(4, itemY - 1, unscaledPanelW - 4, itemY + 12, 0xEE082535);
                    graphics.fill(4, itemY - 1, 6, itemY + 12, accentColor);
                }

                String statusTag = isCompleted ? "DONE"
                        : (isCurrent ? (hasPendingOpActions ? "ACTION REQ" : "RUNNING") : "PENDING");
                int tagColor = isCompleted ? 0xFF55FF55
                        : (isCurrent ? (hasPendingOpActions ? 0xFFFFD700 : 0xFF00E5FF) : 0xFF667788);
                int tagW = font.width(statusTag);

                String prefix = icon + " [" + (i + 1) + "] ";
                String summary = getActionSummary(action.getType(), action.getDetails());
                String actionLine = prefix + summary;

                int maxActionW = unscaledPanelW - tagW - 18;
                if (font.width(actionLine) > maxActionW) {
                    actionLine = font.plainSubstrByWidth(actionLine, Math.max(10, maxActionW - font.width("..")))
                            + "..";
                }

                graphics.drawString(font, actionLine, 8, itemY + 1, textColor, false);
                graphics.drawString(font, statusTag, unscaledPanelW - tagW - 8, itemY + 1, tagColor, false);

                itemY += lineH;
            }

            // Integrated Operator Action Callout Box inside Sequence Panel
            if (hasPendingOpActions) {
                Map.Entry<String, String> currentEntry = opActions.entrySet().iterator().next();
                String triggerId = currentEntry.getKey();
                String label = currentEntry.getValue();
                String promptText = (label != null && !label.isEmpty()) ? label : ("Trigger: " + triggerId);

                int opBoxY = itemY + 2;
                graphics.fill(4, opBoxY, unscaledPanelW - 4, opBoxY + 30, 0xEE1A1208);
                graphics.fill(4, opBoxY, unscaledPanelW - 4, opBoxY + 1, 0xFFFFD700);
                graphics.fill(4, opBoxY + 29, unscaledPanelW - 4, opBoxY + 30, 0xFFFFD700);

                String keyName = ModKeyBindings.OPERATOR_RESUME_KEY.getTranslatedKeyMessage().getString().toUpperCase();
                String reqTitle = "⚠ ACTION REQUIRED: " + promptText;
                int maxReqW = unscaledPanelW - 16;
                if (font.width(reqTitle) > maxReqW) {
                    reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(10, maxReqW - font.width(".."))) + "..";
                }
                graphics.drawString(font, Component.literal(reqTitle).withStyle(ChatFormatting.BOLD), 8, opBoxY + 4,
                        0xFFFFD700, false);

                String holdMsg = "HOLD [" + keyName + "] TO RESUME";
                graphics.drawString(font, Component.literal(holdMsg).withStyle(ChatFormatting.YELLOW), 8, opBoxY + 15,
                        0xFF00E5FF, false);

                // Hold Progress Bar
                int barX = 8;
                int barY = opBoxY + 25;
                int barW = unscaledPanelW - 24;
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
            currentY += scaledPanelH + (int) (6 * scale);
        }

        // Fallback Card: Render standalone Operator Action requirement if no sequence
        // telemetry present
        if (telemetryList.isEmpty() && hasPendingOpActions) {
            Map.Entry<String, String> currentEntry = opActions.entrySet().iterator().next();
            String triggerId = currentEntry.getKey();
            String label = currentEntry.getValue();
            String promptText = (label != null && !label.isEmpty()) ? label : ("Trigger: " + triggerId);

            int unscaledCardH = 44;
            graphics.pose().pushPose();
            graphics.pose().translate(startX, currentY, 200);
            graphics.pose().scale(scale, scale, 1.0f);

            int borderColor = 0xFFFFD700;

            graphics.fill(0, 0, unscaledPanelW, unscaledCardH, bgFill);
            graphics.fill(0, 0, unscaledPanelW, 1, borderColor);
            graphics.fill(0, unscaledCardH - 1, unscaledPanelW, unscaledCardH, borderColor);
            graphics.fill(0, 0, 1, unscaledCardH, borderColor);
            graphics.fill(unscaledPanelW - 1, 0, unscaledPanelW, unscaledCardH, borderColor);

            String keyName = ModKeyBindings.OPERATOR_RESUME_KEY.getTranslatedKeyMessage().getString().toUpperCase();
            graphics.drawString(font, Component.literal("⚠ OPERATOR ACTION REQUIRED").withStyle(ChatFormatting.BOLD), 8,
                    5, borderColor, false);

            String reqTitle = promptText;
            int maxReqW = unscaledPanelW - 16;
            if (font.width(reqTitle) > maxReqW) {
                reqTitle = font.plainSubstrByWidth(reqTitle, Math.max(10, maxReqW - font.width(".."))) + "..";
            }
            graphics.drawString(font, Component.literal(reqTitle), 8, 17, 0xFFFFFFFF, false);

            String holdMsg = "HOLD [" + keyName + "] TO RESUME";
            graphics.drawString(font, Component.literal(holdMsg).withStyle(ChatFormatting.YELLOW), 8, 28, 0xFF00E5FF,
                    false);

            int barX = 8;
            int barY = unscaledCardH - 3;
            int barW = unscaledPanelW - 16;
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

    public static String getActionSummary(String type, String details) {
        if (type == null)
            type = "";
        if (details == null)
            details = "";
        type = type.toLowerCase(Locale.ROOT).trim();
        details = details.trim();

        switch (type) {
            case "command":
                if (details.isEmpty()) {
                    return "CMD: (empty)";
                }
                if (details.startsWith("/")) {
                    return details;
                } else {
                    return "CMD: " + details;
                }
            case "delay":
                return formatDelayDetails(details);
            case "wait_until":
                if (details.startsWith("operator_action:")) {
                    String lbl = details.substring("operator_action:".length()).trim();
                    return "Op Action: " + (lbl.isEmpty() ? "Resume" : lbl);
                } else if (details.startsWith("proximity:")) {
                    String posInfo = details.substring("proximity:".length()).trim();
                    return "Marker: " + posInfo;
                } else if (details.equalsIgnoreCase("proximity") || details.equalsIgnoreCase("marker") || details.equalsIgnoreCase("player_proximity") || details.equalsIgnoreCase("area")) {
                    return "Wait: Player Marker";
                } else if (details.startsWith("trigger:")) {
                    String trig = details.substring("trigger:".length()).trim();
                    return "Trigger: " + trig;
                } else if (details.equalsIgnoreCase("operator_action")) {
                    return "Wait: Op Action";
                } else if (details.equalsIgnoreCase("trigger")) {
                    return "Wait: Trigger Signal";
                } else if (details.equalsIgnoreCase("video") || details.equalsIgnoreCase("video_end")
                        || details.equalsIgnoreCase("cutscene") || details.equalsIgnoreCase("cinematic")) {
                    return "Wait: Video End";
                } else if (details.equalsIgnoreCase("dialog") || details.equalsIgnoreCase("dialog_end")
                        || details.equalsIgnoreCase("dialogs_end") || details.equalsIgnoreCase("dialog_sequence")
                        || details.equalsIgnoreCase("dialog_finish")) {
                    return "Wait: Dialog End";
                } else if (details.startsWith("dialog:")) {
                    String file = details.substring("dialog:".length()).trim();
                    return "Wait: Dialog (" + file + ")";
                } else if (details.equalsIgnoreCase("waiting_room") || details.equalsIgnoreCase("waiting_room_end")
                        || details.equalsIgnoreCase("waitingroom")) {
                    return "Wait: Waiting Room";
                } else if (details.equalsIgnoreCase("downloads") || details.equalsIgnoreCase("downloads_end")
                        || details.equalsIgnoreCase("cutscene_downloads")
                        || details.equalsIgnoreCase("video_downloads")) {
                    return "Wait: Downloads";
                } else if (details.equalsIgnoreCase("waiting_room_ready")
                        || details.equalsIgnoreCase("waiting_room_all_ready")
                        || details.equalsIgnoreCase("waitingroom_ready")) {
                    return "Wait: Players Ready";
                } else if (!details.isEmpty()) {
                    if (isInteger(details)) {
                        return formatDelayDetails(details);
                    }
                    return "Wait: " + details;
                } else {
                    return "Wait Until";
                }
            case "await_trigger":
                return "Trigger: " + (!details.isEmpty() ? details : "signal");
            case "fork_sequence":
                return "Fork: " + (!details.isEmpty() ? details : "sequence");
            case "run_sequence":
                return "Run Seq: " + (!details.isEmpty() ? details : "sequence");
            case "stall_parent":
                return "Stall Parent";
            case "resume_parent":
                return "Resume Parent";
            default:
                String typeName = type.toUpperCase(Locale.ROOT);
                return !details.isEmpty() ? (typeName + ": " + details) : typeName;
        }
    }

    private static String formatDelayDetails(String details) {
        try {
            int ticks = Integer.parseInt(details.trim());
            if (ticks <= 0)
                return "Wait 0s";
            float seconds = ticks / 20.0f;
            if (ticks % 20 == 0) {
                return "Wait " + (ticks / 20) + "s";
            } else {
                return String.format(Locale.ROOT, "Wait %.1fs", seconds);
            }
        } catch (NumberFormatException e) {
            return !details.isEmpty() ? ("Wait " + details) : "Wait";
        }
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
