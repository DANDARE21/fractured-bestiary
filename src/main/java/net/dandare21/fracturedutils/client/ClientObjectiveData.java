package net.dandare21.fracturedutils.client;

public class ClientObjectiveData {
    private static boolean active = false;
    private static String name = "";
    private static String description = "";
    private static String activeWaitText = "";
    private static String waitType = "";
    private static double targetX = 0.0;
    private static double targetY = 0.0;
    private static double targetZ = 0.0;
    private static long timerEndTime = 0;
    private static long objectiveSetTime = 0;

    public static void setObjective(boolean isActive, String objName, String objDesc, String objWaitText, String objWaitType, double x, double y, double z, int remainingTicks) {
        if (isActive && (!active || !name.equals(objName))) {
            objectiveSetTime = System.currentTimeMillis();
        }
        active = isActive;
        name = objName != null ? objName : "";
        description = objDesc != null ? objDesc : "";
        activeWaitText = objWaitText != null ? objWaitText : "";
        waitType = objWaitType != null ? objWaitType : "";
        targetX = x;
        targetY = y;
        targetZ = z;

        if (remainingTicks >= 0) {
            timerEndTime = System.currentTimeMillis() + (remainingTicks * 50L);
        } else {
            timerEndTime = 0;
        }
    }

    public static void setObjective(boolean isActive, String objName, String objDesc, String objWaitText) {
        setObjective(isActive, objName, objDesc, objWaitText, "", 0.0, 0.0, 0.0, -1);
    }

    public static void clearObjective() {
        active = false;
        name = "";
        description = "";
        activeWaitText = "";
        waitType = "";
        targetX = 0.0;
        targetY = 0.0;
        targetZ = 0.0;
        timerEndTime = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static String getName() {
        return name;
    }

    public static String getDescription() {
        return description;
    }

    public static String getActiveWaitText() {
        return activeWaitText;
    }

    public static String getWaitType() {
        return waitType;
    }

    public static double getTargetX() {
        return targetX;
    }

    public static double getTargetY() {
        return targetY;
    }

    public static double getTargetZ() {
        return targetZ;
    }

    public static long getTimerEndTime() {
        return timerEndTime;
    }

    public static long getObjectiveSetTime() {
        return objectiveSetTime;
    }
}
