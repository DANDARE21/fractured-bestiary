package net.dandare21.fracturedutils.sound.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public class ClientAudioConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static float eventMusicVolume = 1.0f;
    private static float eventAmbienceVolume = 1.0f;
    private static boolean loaded = false;

    private static Path getConfigFile() {
        return FMLPaths.GAMEDIR.get().resolve("fractured_utils_cache").resolve("event_audio_options.json");
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    if (json.has("event_music_volume")) {
                        eventMusicVolume = Math.max(0.0f, Math.min(1.0f, json.get("event_music_volume").getAsFloat()));
                    }
                    if (json.has("event_ambience_volume")) {
                        eventAmbienceVolume = Math.max(0.0f, Math.min(1.0f, json.get("event_ambience_volume").getAsFloat()));
                    }
                }
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[ClientAudioConfig] Failed to load sound options", e);
            }
        }
    }

    public static synchronized void save() {
        try {
            Path configFile = getConfigFile();
            if (!Files.exists(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }
            JsonObject json = new JsonObject();
            json.addProperty("event_music_volume", eventMusicVolume);
            json.addProperty("event_ambience_volume", eventAmbienceVolume);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[ClientAudioConfig] Failed to save sound options", e);
        }
    }

    public static float getEventMusicVolume() {
        if (!loaded) load();
        return eventMusicVolume;
    }

    public static void setEventMusicVolume(float vol) {
        eventMusicVolume = Math.max(0.0f, Math.min(1.0f, vol));
        save();
    }

    public static float getEventAmbienceVolume() {
        if (!loaded) load();
        return eventAmbienceVolume;
    }

    public static void setEventAmbienceVolume(float vol) {
        eventAmbienceVolume = Math.max(0.0f, Math.min(1.0f, vol));
        save();
    }

    public static float getChannelVolume(String channel) {
        if (!loaded) load();
        if (channel != null && channel.toLowerCase().contains("ambien")) {
            return eventAmbienceVolume;
        }
        return eventMusicVolume;
    }
}
