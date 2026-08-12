package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.client.gui.WaitingRoomScreen;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.C2SDownloadCompletePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {

    public static void handleSyncState(boolean active, String roomTitle, List<String> playerNames, List<UUID> playerUUIDs, List<Boolean> playerReadyStates, List<Integer> finishedDownloads, List<Integer> remainingDownloads, long elapsedSeconds, boolean isCountingDown, long countdownRemainingSeconds) {
        ClientWaitingRoomData.updateState(active, roomTitle, playerNames, playerUUIDs, playerReadyStates, finishedDownloads, remainingDownloads, elapsedSeconds, isCountingDown, countdownRemainingSeconds);
        Minecraft mc = Minecraft.getInstance();
        if (!active && mc.screen instanceof WaitingRoomScreen) {
            mc.setScreen(null);
        }
    }

    public static void handleOpenWaitingRoomScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof WaitingRoomScreen)) {
            mc.setScreen(new WaitingRoomScreen());
        }
    }

    public static void handlePrepareVideo(String videoUrl, UUID cutsceneId, boolean allowSkip, boolean deleteAfter, String customName) {
        ClientCutsceneHandler.getInstance().prepareVideo(videoUrl, cutsceneId, allowSkip, deleteAfter, customName);
    }

    public static void handleStartPlayback(UUID cutsceneId, long scheduledStartTimeMs) {
        ClientCutsceneHandler.getInstance().startPlayback(cutsceneId, scheduledStartTimeMs);
    }

    public static void handleDownloadVideo(UUID downloadId, String customName, String videoUrl) {
        Minecraft mc = Minecraft.getInstance();
        boolean isOp = mc.player != null && mc.player.hasPermissions(2);

        if (isOp && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§b[Cutscene] Initiating background download for cinematic '" + customName + "'..."));
        }

        ClientVideoCache.downloadNamedVideoAsync(customName, videoUrl)
                .thenAcceptAsync(file -> {
                    if (isOp && mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§a[Cutscene] Successfully downloaded cinematic '" + customName + "'! Ready for instant playback.")
                                .withStyle(ChatFormatting.GREEN));
                    }
                    ModMessages.sendToServer(new C2SDownloadCompletePacket(downloadId, customName, true));
                }, mc)
                .exceptionally(ex -> {
                    if (isOp && mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§c[Cutscene] Failed to download cinematic '" + customName + "': " + ex.getMessage()));
                    }
                    ModMessages.sendToServer(new C2SDownloadCompletePacket(downloadId, customName, false));
                    return null;
                });
    }

    public static void handleSyncPings(List<net.dandare21.fracturedutils.ping.HudPing> pings) {
        ClientPingData.setPings(pings);
    }

    public static void handleSyncDowned(UUID playerUuid, boolean downed, boolean revivingOther, float reviveProgress) {
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(playerUuid);

        if (isLocalPlayer) {
            boolean wasDowned = ClientDownedData.isDowned();
            ClientDownedData.updateState(downed, revivingOther, reviveProgress);
            if (downed) {
                if (!(mc.screen instanceof net.dandare21.fracturedutils.client.gui.DownedSpectateScreen) && !(mc.screen instanceof net.dandare21.fracturedutils.client.gui.TeamWipeScreen)) {
                    mc.setScreen(new net.dandare21.fracturedutils.client.gui.DownedSpectateScreen());
                }
            } else if (wasDowned) {
                if (mc.screen instanceof net.dandare21.fracturedutils.client.gui.DownedSpectateScreen) {
                    mc.setScreen(null);
                }
            }
        }

        ClientDownedData.setPlayerDowned(playerUuid, downed);

        if (mc.level != null) {
            net.minecraft.world.entity.player.Player targetPlayer = mc.level.getPlayerByUUID(playerUuid);
            if (targetPlayer != null) {
                if (downed) {
                    if (!net.dandare21.fracturedutils.client.animation.PlayerAnimationManager.isAnimationPlaying(targetPlayer)) {
                        net.dandare21.fracturedutils.FracturedUtils.LOGGER.info("[PlayerAnim] Triggering playAnimation('startDown') for player {}", targetPlayer.getScoreboardName());
                        net.dandare21.fracturedutils.client.animation.PlayerAnimationManager.playAnimation(targetPlayer, "startDown", true);
                    }
                } else {
                    net.dandare21.fracturedutils.FracturedUtils.LOGGER.info("[PlayerAnim] Triggering stopAnimation for player {}", targetPlayer.getScoreboardName());
                    net.dandare21.fracturedutils.client.animation.PlayerAnimationManager.stopAnimation(targetPlayer);
                }
            }
        }
    }

    public static void handleTeamWipe(int durationSeconds) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new net.dandare21.fracturedutils.client.gui.TeamWipeScreen(durationSeconds));
    }
}
