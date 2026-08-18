package net.dandare21.fracturedutils.sound.util;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
public class OpenALSeeker {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public static void scheduleSeek(SoundInstance instance, long offsetMs) {
        if (instance == null || offsetMs <= 50L) return;

        float offsetSeconds = offsetMs / 1000.0f;

        // Schedule retries over 50ms, 150ms, and 350ms to allow OpenAL stream initialization
        Minecraft.getInstance().execute(() -> {
            boolean ok = attemptSeek(instance, offsetSeconds);
            if (!ok) {
                SCHEDULER.schedule(() -> {
                    Minecraft.getInstance().execute(() -> {
                        boolean ok2 = attemptSeek(instance, offsetSeconds);
                        if (!ok2) {
                            SCHEDULER.schedule(() -> {
                                Minecraft.getInstance().execute(() -> attemptSeek(instance, offsetSeconds));
                            }, 200, TimeUnit.MILLISECONDS);
                        }
                    });
                }, 75, TimeUnit.MILLISECONDS);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static boolean attemptSeek(SoundInstance instance, float offsetSeconds) {
        if (instance == null) return false;

        try {
            SoundManager soundManager = Minecraft.getInstance().getSoundManager();
            if (soundManager == null) return false;

            SoundEngine soundEngine = null;
            for (Field field : SoundManager.class.getDeclaredFields()) {
                if (SoundEngine.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    soundEngine = (SoundEngine) field.get(soundManager);
                    break;
                }
            }
            if (soundEngine == null) return false;

            // Find instanceToChannel map in SoundEngine
            Map<SoundInstance, ?> instanceToChannelMap = null;
            for (Field field : SoundEngine.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object val = field.get(soundEngine);
                    if (val instanceof Map<?, ?> map) {
                        if (!map.isEmpty() && map.containsKey(instance)) {
                            instanceToChannelMap = (Map<SoundInstance, ?>) map;
                            break;
                        }
                    }
                }
            }

            if (instanceToChannelMap == null || !instanceToChannelMap.containsKey(instance)) {
                return false;
            }

            Object token = instanceToChannelMap.get(instance);
            if (token == null) return false;

            int openAlSourceId = extractOpenALSourceId(token);
            if (openAlSourceId > 0) {
                // Ensure OpenAL source is playing/active before setting offset
                int state = AL10.alGetSourcei(openAlSourceId, AL10.AL_SOURCE_STATE);
                if (state != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(openAlSourceId);
                }

                AL10.alSourcef(openAlSourceId, AL11.AL_SEC_OFFSET, offsetSeconds);
                int err = AL10.alGetError();
                if (err == AL10.AL_NO_ERROR) {
                    FracturedUtils.LOGGER.info("[OpenALSeeker] Successfully seeked OpenAL source handle {} to {}s", openAlSourceId, offsetSeconds);
                    return true;
                } else {
                    FracturedUtils.LOGGER.warn("[OpenALSeeker] OpenAL AL_SEC_OFFSET error on source {}: code {}", openAlSourceId, err);
                }
            } else {
                FracturedUtils.LOGGER.warn("[OpenALSeeker] Could not locate valid OpenAL source ID from token: {}", token.getClass().getName());
            }

        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[OpenALSeeker] Error attempting OpenAL seek", e);
        }
        return false;
    }

    private static int extractOpenALSourceId(Object token) {
        if (token == null) return -1;
        try {
            Object channelObj = null;

            // Check fields inside token for Channel object
            for (Field f : token.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(token);
                if (val != null && val.getClass().getName().contains("Channel")) {
                    channelObj = val;
                    break;
                }
            }

            if (channelObj == null) {
                channelObj = token;
            }

            // Search for field named "source" or SRG name "f_82470_" inside com.mojang.blaze3d.audio.Channel
            for (Field f : channelObj.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    String name = f.getName();
                    int val = f.getInt(channelObj);
                    if (name.equals("source") || name.equals("f_82470_") || name.equals("field_217830_a")) {
                        FracturedUtils.LOGGER.info("[OpenALSeeker] Matched OpenAL source field '{}' = {}", name, val);
                        return val;
                    }
                }
            }

            // Fallback: search nested objects inside channelObj
            for (Field f : channelObj.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object child = f.get(channelObj);
                if (child != null && child.getClass().getName().contains("Channel")) {
                    for (Field cf : child.getClass().getDeclaredFields()) {
                        if (cf.getType() == int.class) {
                            cf.setAccessible(true);
                            int cVal = cf.getInt(child);
                            if (cf.getName().equals("source") || cf.getName().equals("f_82470_")) {
                                return cVal;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[OpenALSeeker] Exception extracting source ID", e);
        }
        return -1;
    }
}
