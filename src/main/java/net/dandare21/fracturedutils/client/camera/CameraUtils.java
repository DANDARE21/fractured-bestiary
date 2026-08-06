package net.dandare21.fracturedutils.client.camera;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CameraUtils {
    private static Method setPositionMethod = null;
    private static Method setRotationMethod = null;
    private static Field detachedField = null;
    private static boolean reflectionInitialized = false;

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            for (Method m : Camera.class.getDeclaredMethods()) {
                if (m.getName().equals("setPosition") || m.getName().equals("m_90584_")) {
                    if (m.getParameterCount() == 3 && m.getParameterTypes()[0] == double.class) {
                        m.setAccessible(true);
                        setPositionMethod = m;
                    }
                } else if (m.getName().equals("setRotation") || m.getName().equals("m_90572_")) {
                    if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == float.class) {
                        m.setAccessible(true);
                        setRotationMethod = m;
                    }
                }
            }
            for (Field f : Camera.class.getDeclaredFields()) {
                if (f.getName().equals("detached") || f.getName().equals("m_90581_")) {
                    f.setAccessible(true);
                    detachedField = f;
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public static void applyCameraOverride(Camera camera, ViewportEvent.ComputeCameraAngles event) {
        if (camera == null || !CustomCameraManager.isActive()) return;
        initReflection();

        Vec3 pos;
        float yaw;
        float pitch;
        float roll = CustomCameraManager.getCustomRoll();

        Entity target = CustomCameraManager.getTargetEntity();
        if (target != null && target.isAlive()) {
            float partialTick = (float) event.getPartialTick();
            double targetX = Mth.lerp(partialTick, target.xo, target.getX());
            double targetY = Mth.lerp(partialTick, target.yo, target.getY());
            double targetZ = Mth.lerp(partialTick, target.zo, target.getZ());

            float yHeadRot;
            float yHeadRotO;
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                yHeadRot = living.getYHeadRot();
                yHeadRotO = living.yHeadRotO;
            } else {
                yHeadRot = target.getYRot();
                yHeadRotO = target.yRotO;
            }

            float headYaw = Mth.rotLerp(partialTick, yHeadRotO, yHeadRot);
            float headPitch = Mth.rotLerp(partialTick, target.xRotO, target.getXRot());

            float targetPitch = CustomCameraManager.isOverTheShoulder() ? Mth.clamp(headPitch + 10.0f, -80.0f, 80.0f) : CustomCameraManager.getCustomPitch();

            float smoothYaw = CustomCameraManager.getCurrentSmoothYaw();
            float smoothPitch = CustomCameraManager.getCurrentSmoothPitch();

            if (Float.isNaN(smoothYaw)) {
                smoothYaw = headYaw;
                smoothPitch = targetPitch;
            } else {
                smoothYaw = Mth.rotLerp(0.25f, smoothYaw, headYaw);
                smoothPitch = Mth.rotLerp(0.25f, smoothPitch, targetPitch);
            }
            CustomCameraManager.setCurrentSmoothRotation(smoothYaw, smoothPitch);

            yaw = smoothYaw;
            pitch = smoothPitch;

            Vec3 targetPos;
            if (CustomCameraManager.isOverTheShoulder()) {
                double yawRad = Math.toRadians(smoothYaw);
                double pitchRad = Math.toRadians(smoothPitch);

                double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
                double forwardY = -Math.sin(pitchRad);
                double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);

                double rightX = Math.cos(yawRad);
                double rightZ = Math.sin(yawRad);

                double back = CustomCameraManager.getBackDistance();
                double shoulder = CustomCameraManager.getShoulderOffset();
                double headY = targetY + target.getEyeHeight() + CustomCameraManager.getHeightOffset();

                double camX = targetX - (forwardX * back) + (rightX * shoulder);
                double camY = headY - (forwardY * back);
                double camZ = targetZ - (forwardZ * back) + (rightZ * shoulder);
                targetPos = new Vec3(camX, camY, camZ);
            } else {
                double camY = targetY + CustomCameraManager.getHeightOffset();
                targetPos = new Vec3(targetX, camY, targetZ);
            }

            Vec3 current = CustomCameraManager.getCurrentSmoothPos();
            if (current == null) {
                current = targetPos;
            } else {
                double x = current.x + (targetPos.x - current.x) * 0.25;
                double y = current.y + (targetPos.y - current.y) * 0.25;
                double z = current.z + (targetPos.z - current.z) * 0.25;
                current = new Vec3(x, y, z);
            }
            CustomCameraManager.setCurrentSmoothPos(current);
            pos = current;
        } else {
            pos = CustomCameraManager.getCustomPosition();
            yaw = CustomCameraManager.getCustomYaw();
            pitch = CustomCameraManager.getCustomPitch();
        }

        event.setPitch(pitch);
        event.setYaw(yaw);
        event.setRoll(roll);

        try {
            if (setPositionMethod != null) {
                setPositionMethod.invoke(camera, pos.x, pos.y, pos.z);
            }
            if (setRotationMethod != null) {
                setRotationMethod.invoke(camera, yaw, pitch);
            }
            if (detachedField != null && CustomCameraManager.shouldRenderPlayerModel()) {
                detachedField.setBoolean(camera, true);
            }
        } catch (Exception ignored) {}
    }

    public static void setOverheadCamera(Entity target, double heightOffset, float pitch) {
        if (target == null) return;
        CustomCameraManager.setTargetEntity(target, heightOffset, pitch);
    }

    public static void setModernThirdPersonCamera(Entity target) {
        if (target == null) return;
        CustomCameraManager.setOverTheShoulderTarget(target, 2.2, 0.45, 0.1, 15.0f);
    }
}
