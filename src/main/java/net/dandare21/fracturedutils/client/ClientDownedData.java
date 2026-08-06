package net.dandare21.fracturedutils.client;

public class ClientDownedData {
    private static boolean downed = false;
    private static boolean revivingOther = false;
    private static float reviveProgress = 0.0f;

    public static synchronized void updateState(boolean isDowned, boolean isRevivingOther, float progress) {
        downed = isDowned;
        revivingOther = isRevivingOther;
        reviveProgress = progress;
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
