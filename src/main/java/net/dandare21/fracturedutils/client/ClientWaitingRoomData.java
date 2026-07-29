package net.dandare21.fracturedutils.client;

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

    public static synchronized void updateState(boolean isActive, String title, List<String> names, List<UUID> uuids, List<Boolean> readyStates) {
        active = isActive;
        roomTitle = title != null ? title : "Starting Soon...";
        playerNames = names != null ? new ArrayList<>(names) : new ArrayList<>();
        playerUUIDs = uuids != null ? new ArrayList<>(uuids) : new ArrayList<>();
        playerReadyStates = readyStates != null ? new ArrayList<>(readyStates) : new ArrayList<>();
    }

    public static boolean isActive() {
        return active;
    }

    public static String getRoomTitle() {
        return roomTitle;
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
