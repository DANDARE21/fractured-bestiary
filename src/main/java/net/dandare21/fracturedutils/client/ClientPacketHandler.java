package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.client.gui.WaitingRoomScreen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {

    public static void handleSyncState(boolean active, String roomTitle, List<String> playerNames, List<UUID> playerUUIDs, List<Boolean> playerReadyStates) {
        ClientWaitingRoomData.updateState(active, roomTitle, playerNames, playerUUIDs, playerReadyStates);
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
}
