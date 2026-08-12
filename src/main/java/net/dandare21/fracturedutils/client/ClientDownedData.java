package net.dandare21.fracturedutils.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDownedData {
    private static boolean downed = false;
    private static boolean revivingOther = false;
    private static float reviveProgress = 0.0f;
    private static final Set<UUID> downedPlayerUuids = ConcurrentHashMap.newKeySet();

    public static synchronized void updateState(boolean isDowned, boolean isRevivingOther, float progress) {
        downed = isDowned;
        revivingOther = isRevivingOther;
        reviveProgress = progress;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            setPlayerDowned(mc.player.getUUID(), isDowned);
        }
    }

    public static void setPlayerDowned(UUID uuid, boolean isDowned) {
        if (uuid == null) return;
        if (isDowned) {
            downedPlayerUuids.add(uuid);
        } else {
            downedPlayerUuids.remove(uuid);
        }
    }

    public static boolean isPlayerDowned(UUID uuid) {
        if (uuid == null) return false;
        return downedPlayerUuids.contains(uuid);
    }

    public static boolean isDowned() {
        return downed;
    }

    public static boolean isRevivingOther() {
        return revivingOther;
    }

    public static float getReviveProgress() {
        return reviveProgress;
    }
}
