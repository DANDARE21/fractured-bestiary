package net.dandare21.fracturedutils.sound.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@OnlyIn(Dist.CLIENT)
public class DynamicSoundRegistry {

    private static Map<ResourceLocation, WeighedSoundEvents> cachedSoundRegistry = null;

    @SuppressWarnings("unchecked")
    public static Map<ResourceLocation, WeighedSoundEvents> getSoundRegistry(SoundManager soundManager) {
        if (cachedSoundRegistry != null) return cachedSoundRegistry;
        try {
            for (Field field : SoundManager.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object val = field.get(soundManager);
                    if (val instanceof Map<?, ?> map) {
                        cachedSoundRegistry = (Map<ResourceLocation, WeighedSoundEvents>) map;
                        return cachedSoundRegistry;
                    }
                }
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[DynamicSoundRegistry] Failed to reflectively access SoundManager registry", e);
        }
        return null;
    }

    public static synchronized boolean registerSoundPackZip(File zipFile, String namespace) {
        if (zipFile == null || !zipFile.exists()) return false;

        try {
            // 1. Inject SoundStreamPackResources into FallbackResourceManager
            injectPackResourcesIntoResourceManager(zipFile, namespace);

            // 2. Parse sounds.json and inject WeighedSoundEvents into SoundManager.registry
            try (ZipFile zf = new ZipFile(zipFile)) {
                String soundsJsonPath = "assets/" + namespace + "/sounds.json";
                ZipEntry entry = zf.getEntry(soundsJsonPath);
                if (entry == null) {
                    soundsJsonPath = "assets/fracturedutils/sounds.json";
                    entry = zf.getEntry(soundsJsonPath);
                }

                if (entry != null) {
                    try (InputStream is = zf.getInputStream(entry);
                         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
                        Map<ResourceLocation, WeighedSoundEvents> registry = getSoundRegistry(soundManager);

                        if (registry != null) {
                            int count = 0;
                            for (Map.Entry<String, JsonElement> jsonEntry : root.entrySet()) {
                                String eventKey = jsonEntry.getKey(); // e.g. "event.track1"
                                JsonObject eventObj = jsonEntry.getValue().getAsJsonObject();

                                ResourceLocation primaryLoc = eventKey.contains(":")
                                        ? new ResourceLocation(eventKey)
                                        : new ResourceLocation(namespace, eventKey);

                                WeighedSoundEvents weighed = new WeighedSoundEvents(primaryLoc, "Custom Dynamic Audio");

                                if (eventObj.has("sounds")) {
                                    JsonArray soundsArray = eventObj.getAsJsonArray("sounds");
                                    for (JsonElement sElem : soundsArray) {
                                        String soundPathStr = "";
                                        boolean stream = true;

                                        if (sElem.isJsonObject()) {
                                            JsonObject sObj = sElem.getAsJsonObject();
                                            soundPathStr = sObj.get("name").getAsString();
                                            if (sObj.has("stream")) {
                                                stream = sObj.get("stream").getAsBoolean();
                                            }
                                        } else if (sElem.isJsonPrimitive()) {
                                            soundPathStr = sElem.getAsString();
                                        }

                                        if (!soundPathStr.isEmpty()) {
                                            // MANDATORY: Sound.Type.FILE ensures OpenAL reads .ogg from FallbackResourceManager
                                            Sound sound = new Sound(
                                                    soundPathStr,
                                                    ConstantFloat.of(1.0f),
                                                    ConstantFloat.of(1.0f),
                                                    1,
                                                    Sound.Type.FILE,
                                                    stream,
                                                    false,
                                                    16
                                            );
                                            weighed.addSound(sound);
                                        }
                                    }
                                }

                                // Register primary location e.g. fracturedutils:event.track1
                                registry.put(primaryLoc, weighed);

                                // Register aliases so "track1", "event.track1", "fracturedutils:track1" all work!
                                if (eventKey.startsWith("event.")) {
                                    String shortName = eventKey.substring(6); // e.g. "track1"
                                    registry.put(new ResourceLocation(namespace, shortName), weighed);
                                    registry.put(new ResourceLocation("fracturedutils", shortName), weighed);
                                }
                                registry.put(new ResourceLocation("fracturedutils", eventKey), weighed);

                                count++;
                            }
                            FracturedUtils.LOGGER.info("[DynamicSoundRegistry] Successfully registered {} custom sound event(s) and aliases into SoundManager.", count);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[DynamicSoundRegistry] Exception dynamically registering sound pack", e);
        }
        return false;
    }

    private static void injectPackResourcesIntoResourceManager(File zipFile, String namespace) {
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            SoundStreamPackResources packResources = new SoundStreamPackResources("fractured_utils_event_audio", zipFile, namespace);

            for (Field field : rm.getClass().getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object val = field.get(rm);
                    if (val instanceof Map<?, ?> map) {
                        int injectedCount = 0;
                        for (Object mgr : map.values()) {
                            if (mgr instanceof FallbackResourceManager fallbackMgr) {
                                fallbackMgr.push(packResources);
                                injectedCount++;
                            }
                        }
                        FracturedUtils.LOGGER.info("[DynamicSoundRegistry] Injected SoundStreamPackResources into {} FallbackResourceManager instance(s)", injectedCount);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[DynamicSoundRegistry] Failed to inject SoundStreamPackResources into FallbackResourceManager", e);
        }
    }
}
