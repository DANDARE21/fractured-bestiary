package net.dandare21.fracturedutils.client.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class CustomCameraManager {
    private static boolean active = false;
    private static Vec3 customPosition = Vec3.ZERO;
    private static float customYaw = 0.0f;
    private static float customPitch = 0.0f;
    private static float customRoll = 0.0f;
    private static boolean renderPlayerModel = true;

    private static Entity targetEntity = null;
    private static double heightOffset = 5.5;
    private static boolean overTheShoulder = false;
    private static double backDistance = 2.4;
    private static double shoulderOffset = 0.55;
    private static Vec3 currentSmoothPos = null;

    private static float currentSmoothYaw = Float.NaN;
    private static float currentSmoothPitch = Float.NaN;

    public static void setCustomCamera(Vec3 pos, float yaw, float pitch, float roll, boolean renderPlayerModel) {
        CustomCameraManager.targetEntity = null;
        CustomCameraManager.overTheShoulder = false;
        CustomCameraManager.customPosition = pos;
        CustomCameraManager.customYaw = yaw;
        CustomCameraManager.customPitch = pitch;
        CustomCameraManager.customRoll = roll;
        CustomCameraManager.renderPlayerModel = renderPlayerModel;
        CustomCameraManager.active = true;
    }

    public static void setCustomCamera(double x, double y, double z, float yaw, float pitch, boolean renderPlayerModel) {
        setCustomCamera(new Vec3(x, y, z), yaw, pitch, 0.0f, renderPlayerModel);
    }

    public static void setCustomCamera(double x, double y, double z, float yaw, float pitch) {
        setCustomCamera(x, y, z, yaw, pitch, true);
    }

    public static void setTargetEntity(Entity target, double heightOffset, float pitch) {
        CustomCameraManager.targetEntity = target;
        CustomCameraManager.overTheShoulder = false;
        CustomCameraManager.heightOffset = heightOffset;
        CustomCameraManager.customPitch = pitch;
        if (target != null) {
            CustomCameraManager.customYaw = target.getYRot();
        }
        CustomCameraManager.renderPlayerModel = true;
        CustomCameraManager.active = true;
    }

    public static void setOverTheShoulderTarget(Entity target, double backDistance, double shoulderOffset, double heightOffset, float pitch) {
        CustomCameraManager.targetEntity = target;
        CustomCameraManager.overTheShoulder = true;
        CustomCameraManager.backDistance = backDistance;
        CustomCameraManager.shoulderOffset = shoulderOffset;
        CustomCameraManager.heightOffset = heightOffset;
        CustomCameraManager.customPitch = pitch;
        if (target != null) {
            CustomCameraManager.customYaw = target.getYRot();
        }
        CustomCameraManager.renderPlayerModel = true;
        CustomCameraManager.active = true;
    }

    private static boolean customFovActive = false;
    private static double customFov = 70.0;

    public static void setCustomFov(double fov) {
        CustomCameraManager.customFov = fov;
        CustomCameraManager.customFovActive = true;
    }

    public static void clearCustomFov() {
        CustomCameraManager.customFovActive = false;
        CustomCameraManager.customFov = 70.0;
    }

    public static boolean isFovActive() {
        return customFovActive;
    }

    public static double getCustomFov() {
        return customFov;
    }

    public static void clearCustomCamera() {
        CustomCameraManager.active = false;
        CustomCameraManager.targetEntity = null;
        CustomCameraManager.overTheShoulder = false;
        CustomCameraManager.currentSmoothPos = null;
        CustomCameraManager.currentSmoothYaw = Float.NaN;
        CustomCameraManager.currentSmoothPitch = Float.NaN;
        CustomCameraManager.customFovActive = false;
        CustomCameraManager.customFov = 70.0;
    }

    public static boolean isActive() {
        return active;
    }

    public static Entity getTargetEntity() {
        return targetEntity;
    }

    public static boolean isOverTheShoulder() {
        return overTheShoulder;
    }

    public static double getBackDistance() {
        return backDistance;
    }

    public static double getShoulderOffset() {
        return shoulderOffset;
    }

    public static double getHeightOffset() {
        return heightOffset;
    }

    public static Vec3 getCurrentSmoothPos() {
        return currentSmoothPos;
    }

    public static void setCurrentSmoothPos(Vec3 pos) {
        currentSmoothPos = pos;
    }

    public static float getCurrentSmoothYaw() {
        return currentSmoothYaw;
    }

    public static float getCurrentSmoothPitch() {
        return currentSmoothPitch;
    }

    public static void setCurrentSmoothRotation(float yaw, float pitch) {
        currentSmoothYaw = yaw;
        currentSmoothPitch = pitch;
    }

    public static Vec3 getCustomPosition() {
        return customPosition;
    }

    public static float getCustomYaw() {
        return customYaw;
    }

    public static float getCustomPitch() {
        return customPitch;
    }

    public static float getCustomRoll() {
        return customRoll;
    }

    public static boolean shouldRenderPlayerModel() {
        return renderPlayerModel;
    }
}
