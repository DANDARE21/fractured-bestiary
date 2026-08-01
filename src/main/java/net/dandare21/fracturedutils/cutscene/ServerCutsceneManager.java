package net.dandare21.fracturedutils.cutscene;

import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.S2CDownloadVideoPacket;
import net.dandare21.fracturedutils.network.packet.S2CPrepareVideoPacket;
import net.dandare21.fracturedutils.network.packet.S2CStartPlaybackPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.*;

public class ServerCutsceneManager {
    private static final ServerCutsceneManager INSTANCE = new ServerCutsceneManager();

    private final Map<UUID, Set<UUID>> expectedPlayersMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> readyPlayersMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private final Set<UUID> activeCutscenePlayers = ConcurrentHashMap.newKeySet();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cutscene-Manager-Timeout-Worker");
        t.setDaemon(true);
        return t;
    });

    public static ServerCutsceneManager getInstance() {
        return INSTANCE;
    }

    public boolean isPlayerInCutscene(UUID playerUuid) {
        return playerUuid != null && activeCutscenePlayers.contains(playerUuid);
    }

    public synchronized void onClientCutsceneEnd(ServerPlayer player, UUID cutsceneId) {
        if (player != null) {
            activeCutscenePlayers.remove(player.getUUID());
        }
    }

    private final Map<UUID, Set<UUID>> expectedDownloadPlayersMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> completedDownloadPlayersMap = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> failedDownloadPlayersMap = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> finishedDownloadsMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> remainingDownloadsMap = new ConcurrentHashMap<>();

    public int getFinishedDownloads(UUID uuid) {
        return uuid != null ? finishedDownloadsMap.getOrDefault(uuid, 0) : 0;
    }

    public int getRemainingDownloads(UUID uuid) {
        return uuid != null ? remainingDownloadsMap.getOrDefault(uuid, 0) : 0;
    }

    public void resetDownloadCounters() {
        finishedDownloadsMap.clear();
        remainingDownloadsMap.clear();
        expectedDownloadPlayersMap.clear();
        completedDownloadPlayersMap.clear();
        failedDownloadPlayersMap.clear();
    }

    public void startCinematicDownload(MinecraftServer server, Collection<ServerPlayer> players, String name, String url) {
        if (server == null || players == null || players.isEmpty() || name == null || url == null) return;
        ServerCutsceneSavedData savedData = ServerCutsceneSavedData.get(server);
        savedData.registerCutscene(name, url);

        UUID downloadId = UUID.randomUUID();
        Set<UUID> expectedSet = ConcurrentHashMap.newKeySet();
        Set<UUID> completedSet = ConcurrentHashMap.newKeySet();
        Set<UUID> failedSet = ConcurrentHashMap.newKeySet();

        for (ServerPlayer player : players) {
            if (player != null && player.connection != null) {
                UUID uuid = player.getUUID();
                expectedSet.add(uuid);
                remainingDownloadsMap.put(uuid, getRemainingDownloads(uuid) + 1);
            }
        }

        if (expectedSet.isEmpty()) return;

        expectedDownloadPlayersMap.put(downloadId, expectedSet);
        completedDownloadPlayersMap.put(downloadId, completedSet);
        failedDownloadPlayersMap.put(downloadId, failedSet);

        for (ServerPlayer player : players) {
            if (player != null && expectedSet.contains(player.getUUID())) {
                ModMessages.sendToPlayer(new S2CDownloadVideoPacket(downloadId, name, url), player);
            }
        }

        if (net.dandare21.fracturedutils.waitingroom.WaitingRoomManager.getInstance().isActive()) {
            net.dandare21.fracturedutils.waitingroom.WaitingRoomManager.getInstance().syncToAll(server);
        }
    }

    public synchronized void onClientDownloadComplete(ServerPlayer player, UUID downloadId, String customName, boolean success) {
        Set<UUID> expectedSet = expectedDownloadPlayersMap.get(downloadId);
        Set<UUID> completedSet = completedDownloadPlayersMap.get(downloadId);
        Set<UUID> failedSet = failedDownloadPlayersMap.get(downloadId);

        if (player != null) {
            UUID uuid = player.getUUID();
            int currentRemaining = getRemainingDownloads(uuid);
            remainingDownloadsMap.put(uuid, Math.max(0, currentRemaining - 1));
            if (success) {
                finishedDownloadsMap.put(uuid, getFinishedDownloads(uuid) + 1);
            }
            if (net.dandare21.fracturedutils.waitingroom.WaitingRoomManager.getInstance().isActive()) {
                net.dandare21.fracturedutils.waitingroom.WaitingRoomManager.getInstance().syncToAll(player.getServer());
            }
        }

        if (expectedSet == null || completedSet == null || failedSet == null) {
            return;
        }

        expectedSet.remove(player.getUUID());
        if (success) {
            completedSet.add(player.getUUID());
        } else {
            failedSet.add(player.getUUID());
        }

        if (expectedSet.isEmpty()) {
            int totalCount = completedSet.size() + failedSet.size();
            int successCount = completedSet.size();
            int failCount = failedSet.size();

            expectedDownloadPlayersMap.remove(downloadId);
            completedDownloadPlayersMap.remove(downloadId);
            failedDownloadPlayersMap.remove(downloadId);

            MinecraftServer server = player.getServer();
            if (server != null) {
                Component notification;
                if (failCount == 0) {
                    notification = Component.literal("§a[Cutscene] All " + totalCount + " player(s) have finished downloading cinematic '" + customName + "'!")
                            .withStyle(ChatFormatting.GREEN);
                } else {
                    notification = Component.literal("§e[Cutscene] Cinematic '" + customName + "' download finished for all players (" + successCount + " succeeded, " + failCount + " failed).")
                            .withStyle(ChatFormatting.YELLOW);
                }

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (p != null && p.hasPermissions(2)) {
                        p.sendSystemMessage(notification);
                    }
                }
            }
        }
    }

    public static void startCinematicPreparation(Collection<ServerPlayer> players, String media, boolean allowSkip, boolean deleteAfter) {
        MinecraftServer server = players != null && !players.isEmpty() ? players.iterator().next().getServer() : null;
        getInstance().startPreparation(server, players, media, allowSkip, deleteAfter);
    }

    public static void startCinematicPreparation(MinecraftServer server, Collection<ServerPlayer> players, String media, boolean allowSkip, boolean deleteAfter) {
        getInstance().startPreparation(server, players, media, allowSkip, deleteAfter);
    }

    public synchronized void startPreparation(MinecraftServer server, Collection<ServerPlayer> players, String media, boolean allowSkip, boolean deleteAfter) {
        if (players == null || players.isEmpty()) {
            return;
        }

        String url = media;
        String customName = null;
        if (server != null) {
            ServerCutsceneSavedData savedData = ServerCutsceneSavedData.get(server);
            String registeredUrl = savedData.getUrl(media);
            if (registeredUrl != null) {
                url = registeredUrl;
                customName = media;
            }
        }

        UUID cutsceneId = UUID.randomUUID();
        Set<UUID> expectedSet = ConcurrentHashMap.newKeySet();
        Set<UUID> readySet = ConcurrentHashMap.newKeySet();

        for (ServerPlayer player : players) {
            if (player != null && player.connection != null) {
                expectedSet.add(player.getUUID());
            }
        }

        if (expectedSet.isEmpty()) {
            return;
        }

        expectedPlayersMap.put(cutsceneId, expectedSet);
        readyPlayersMap.put(cutsceneId, readySet);
        activeCutscenePlayers.addAll(expectedSet);

        // Send prepare packet to all target players
        for (ServerPlayer player : players) {
            if (player != null && expectedSet.contains(player.getUUID())) {
                ModMessages.sendToPlayer(new S2CPrepareVideoPacket(url, cutsceneId, allowSkip, deleteAfter, customName), player);
            }
        }

        // Schedule 30 second timeout fallback
        ScheduledFuture<?> timeoutTask = executorService.schedule(() -> {
            onTimeout(cutsceneId);
        }, 30, TimeUnit.SECONDS);

        timeoutTasks.put(cutsceneId, timeoutTask);
    }

    public synchronized void onClientReady(ServerPlayer player, UUID cutsceneId) {
        if (player == null || cutsceneId == null) return;
        UUID playerUuid = player.getUUID();

        Set<UUID> expected = expectedPlayersMap.get(cutsceneId);
        Set<UUID> ready = readyPlayersMap.get(cutsceneId);

        if (expected != null && expected.contains(playerUuid) && ready != null) {
            ready.add(playerUuid);

            // If all expected players are ready, trigger playback immediately
            if (ready.containsAll(expected)) {
                triggerPlayback(cutsceneId, false, player.getServer());
            }
        }
    }

    public synchronized void onPlayerLoggedOut(ServerPlayer player) {
        if (player == null) return;
        UUID playerUuid = player.getUUID();
        MinecraftServer server = player.getServer();

        for (UUID cutsceneId : new ArrayList<>(expectedPlayersMap.keySet())) {
            Set<UUID> expected = expectedPlayersMap.get(cutsceneId);
            Set<UUID> ready = readyPlayersMap.get(cutsceneId);

            if (expected != null && expected.remove(playerUuid)) {
                if (ready != null) {
                    ready.remove(playerUuid);
                }

                // If expected set is now empty, clean up
                if (expected.isEmpty()) {
                    cancelTimeout(cutsceneId);
                    cleanup(cutsceneId);
                } else if (ready != null && ready.containsAll(expected)) {
                    // All remaining players are ready
                    triggerPlayback(cutsceneId, false, server);
                }
            }
        }
    }

    private synchronized void onTimeout(UUID cutsceneId) {
        // Fallback: timer expired after 30 seconds
        triggerPlayback(cutsceneId, true, null);
    }

    private synchronized void triggerPlayback(UUID cutsceneId, boolean isTimeout, MinecraftServer fallbackServer) {
        cancelTimeout(cutsceneId);

        Set<UUID> readySet = readyPlayersMap.get(cutsceneId);
        Set<UUID> expectedSet = expectedPlayersMap.get(cutsceneId);

        if (readySet != null && expectedSet != null) {
            Set<UUID> targetPlayers = isTimeout ? new HashSet<>(readySet) : new HashSet<>(expectedSet);
            if (!targetPlayers.isEmpty()) {
                // Set scheduled start time ~1.5s (1500ms) in the future to absorb network latency
                long scheduledStartTimeMs = System.currentTimeMillis() + 1500L;

                for (UUID playerUuid : targetPlayers) {
                    ServerPlayer player = findOnlinePlayer(playerUuid, fallbackServer);
                    if (player != null) {
                        ModMessages.sendToPlayer(new S2CStartPlaybackPacket(cutsceneId, scheduledStartTimeMs), player);
                    }
                }
            }
        }

        cleanup(cutsceneId);
    }

    private ServerPlayer findOnlinePlayer(UUID uuid, MinecraftServer fallbackServer) {
        if (fallbackServer != null && fallbackServer.getPlayerList() != null) {
            return fallbackServer.getPlayerList().getPlayer(uuid);
        }
        return null;
    }

    private void cancelTimeout(UUID cutsceneId) {
        ScheduledFuture<?> task = timeoutTasks.remove(cutsceneId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    private void cleanup(UUID cutsceneId) {
        expectedPlayersMap.remove(cutsceneId);
        readyPlayersMap.remove(cutsceneId);
        timeoutTasks.remove(cutsceneId);
    }
}
