package net.dandare21.fracturedutils.client;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClientWaitingRoomData {
    private static boolean active = false;
    private static String roomTitle = "Starting Soon...";
    private static List<String> playerNames = new ArrayList<>();
    private static List<UUID> playerUUIDs = new ArrayList<>();
    private static List<Boolean> playerReadyStates = new ArrayList<>();
    private static long startTimeLocalMs = 0;
    private static boolean countingDown = false;
    private static long clientCountdownEndMs = 0;
    private static final List<Component> chatMessages = new ArrayList<>();

    public static synchronized void updateState(boolean isActive, String title, List<String> names, List<UUID> uuids, List<Boolean> readyStates, long elapsedSeconds, boolean isCountingDown, long countdownRemainingSeconds) {
        active = isActive;
        roomTitle = title != null ? title : "Starting Soon...";
        playerNames = names != null ? new ArrayList<>(names) : new ArrayList<>();
        playerUUIDs = uuids != null ? new ArrayList<>(uuids) : new ArrayList<>();
        playerReadyStates = readyStates != null ? new ArrayList<>(readyStates) : new ArrayList<>();
        startTimeLocalMs = System.currentTimeMillis() - (elapsedSeconds * 1000);
        countingDown = isCountingDown;
        if (isCountingDown) {
            clientCountdownEndMs = System.currentTimeMillis() + (countdownRemainingSeconds * 1000);
        } else {
            clientCountdownEndMs = 0;
        }
    }

    public static synchronized void addChatMessage(Component message) {
        if (message == null) return;
        chatMessages.add(message);
        while (chatMessages.size() > 100) {
            chatMessages.remove(0);
        }
    }

    public static synchronized List<Component> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isCountingDown() {
        return active && countingDown;
    }

    public static long getCountdownRemainingSeconds() {
        if (!active || !countingDown) return 0;
        return Math.max(0, (clientCountdownEndMs - System.currentTimeMillis() + 999) / 1000);
    }

    public static String getRoomTitle() {
        return roomTitle;
    }

    public static long getElapsedSeconds() {
        if (!active) return 0;
        return Math.max(0, (System.currentTimeMillis() - startTimeLocalMs) / 1000);
    }

    public static List<String> getPlayerNames() {
        return Collections.unmodifiableList(playerNames);
    }

    public static List<UUID> getPlayerUUIDs() {
        return Collections.unmodifiableList(playerUUIDs);
    }

    public static List<Boolean> getPlayerReadyStates() {
        return Collections.unmodifiableList(playerReadyStates);
    }

    public static boolean isSelfJoined(UUID selfUUID) {
        if (selfUUID == null) return false;
        return playerUUIDs.contains(selfUUID);
    }

    public static boolean isSelfReady(UUID selfUUID) {
        if (selfUUID == null) return false;
        int idx = playerUUIDs.indexOf(selfUUID);
        if (idx >= 0 && idx < playerReadyStates.size()) {
            return playerReadyStates.get(idx);
        }
        return false;
    }
}
