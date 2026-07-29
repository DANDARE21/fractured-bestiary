package net.dandare21.fracturedutils.waitingroom;

import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.OpenWaitingRoomScreenS2CPacket;
import net.dandare21.fracturedutils.network.packet.SyncWaitingRoomStateS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WaitingRoomManager {
    private static final WaitingRoomManager INSTANCE = new WaitingRoomManager();

    private boolean active = false;
    private String roomTitle = "Starting Soon...";
    private long startTimeMs = 0;
    private boolean countingDown = false;
    private long countdownEndMs = 0;
    private final Set<UUID> joinedPlayers = new HashSet<>();
    private final Set<UUID> readyPlayers = new HashSet<>();

    public static WaitingRoomManager getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCountingDown() {
        return active && countingDown;
    }

    public long getCountdownRemainingSeconds() {
        if (!active || !countingDown) return 0;
        return Math.max(0, (countdownEndMs - System.currentTimeMillis() + 999) / 1000);
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public Set<UUID> getJoinedPlayers() {
        return joinedPlayers;
    }

    public boolean isPlayerJoined(UUID uuid) {
        return joinedPlayers.contains(uuid);
    }

    public boolean isPlayerReady(UUID uuid) {
        return readyPlayers.contains(uuid);
    }

    public void start(MinecraftServer server, String title) {
        this.active = true;
        this.countingDown = false;
        this.countdownEndMs = 0;
        this.roomTitle = (title != null && !title.isBlank()) ? title : "Starting Soon...";
        this.startTimeMs = System.currentTimeMillis();
        this.joinedPlayers.clear();
        this.readyPlayers.clear();

        syncToAll(server);

        Component announcement = Component.literal("[Event Waiting Room] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("The waiting room for '" + this.roomTitle + "' is now OPEN! ")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("HOLD [J]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" to enter the waiting room screen (No going back!).").withStyle(ChatFormatting.YELLOW));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(announcement);
            }
        }
    }

    public void startCountdown(MinecraftServer server, int seconds) {
        if (!this.active) {
            start(server, "Starting Soon...");
        }
        this.countingDown = true;
        this.countdownEndMs = System.currentTimeMillis() + (seconds * 1000L);
        syncToAll(server);
    }

    public void tick(MinecraftServer server) {
        if (this.active && this.countingDown) {
            long remainingMs = this.countdownEndMs - System.currentTimeMillis();
            if (remainingMs <= 0) {
                stop(server);
            }
        }
    }

    public void stop(MinecraftServer server) {
        this.active = false;
        this.countingDown = false;
        this.countdownEndMs = 0;
        this.startTimeMs = 0;
        this.joinedPlayers.clear();
        this.readyPlayers.clear();
        syncToAll(server);

        Component announcement = Component.literal("[Event Waiting Room] ")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("The waiting room has been CLOSED.").withStyle(ChatFormatting.RED));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(announcement);
            }
        }
    }

    public void joinPlayer(ServerPlayer player) {
        if (!active) return;
        boolean added = joinedPlayers.add(player.getUUID());
        ModMessages.sendToPlayer(new OpenWaitingRoomScreenS2CPacket(), player);
        if (added) {
            syncToAll(player.getServer());
        }
    }

    public boolean toggleReady(ServerPlayer player) {
        if (!active) return false;
        UUID uuid = player.getUUID();
        joinedPlayers.add(uuid);

        boolean isNowReady;
        if (readyPlayers.contains(uuid)) {
            readyPlayers.remove(uuid);
            isNowReady = false;
        } else {
            readyPlayers.add(uuid);
            isNowReady = true;
        }

        syncToAll(player.getServer());
        return isNowReady;
    }

    public void leavePlayer(ServerPlayer player) {
        if (!active) return;
        UUID uuid = player.getUUID();
        joinedPlayers.remove(uuid);
        readyPlayers.remove(uuid);
        syncToAll(player.getServer());
    }

    public void removePlayerByUUID(MinecraftServer server, UUID uuid) {
        if (!active) return;
        boolean removed = joinedPlayers.remove(uuid);
        readyPlayers.remove(uuid);
        if (removed) {
            syncToAll(server);
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        SyncWaitingRoomStateS2CPacket packet = createSyncPacket(player.getServer());
        ModMessages.sendToPlayer(packet, player);
    }

    public void syncToAll(MinecraftServer server) {
        if (server == null) return;
        SyncWaitingRoomStateS2CPacket packet = createSyncPacket(server);
        ModMessages.sendToAllPlayers(packet, server);
    }

    public SyncWaitingRoomStateS2CPacket createSyncPacket(MinecraftServer server) {
        List<String> playerNames = new ArrayList<>();
        List<UUID> playerUUIDs = new ArrayList<>();
        List<Boolean> readyStates = new ArrayList<>();

        if (server != null) {
            for (UUID uuid : joinedPlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    playerNames.add(player.getScoreboardName());
                    playerUUIDs.add(uuid);
                    readyStates.add(readyPlayers.contains(uuid));
                }
            }
        }

        long elapsedSeconds = active ? Math.max(0, (System.currentTimeMillis() - startTimeMs) / 1000) : 0;
        long countdownRemaining = isCountingDown() ? getCountdownRemainingSeconds() : 0;
        return new SyncWaitingRoomStateS2CPacket(active, roomTitle, playerNames, playerUUIDs, readyStates, elapsedSeconds, countingDown, countdownRemaining);
    }
}
