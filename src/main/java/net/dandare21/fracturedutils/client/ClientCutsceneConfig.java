package net.dandare21.fracturedutils.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientCutsceneConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("fracturedutils-client.json");

    private static int videoVolumePercent = 100;
    private static boolean loaded = false;

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null && json.has("videoVolumePercent")) {
                        videoVolumePercent = Math.max(0, Math.min(100, json.get("videoVolumePercent").getAsInt()));
                        FracturedUtils.LOGGER.info("[CutsceneConfig] Loaded saved videoVolumePercent: " + videoVolumePercent);
                    }
                }
            } else {
                save();
            }
            loaded = true;
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[CutsceneConfig] Failed to load client config", e);
        }
    }

    public static void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("videoVolumePercent", videoVolumePercent);
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[CutsceneConfig] Failed to save client config", e);
        }
    }

    public static int getVideoVolumePercent() {
        if (!loaded) {
            load();
        }
        return videoVolumePercent;
    }

    public static void setVideoVolumePercent(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        if (videoVolumePercent != clamped || !loaded) {
            videoVolumePercent = clamped;
            loaded = true;
            save();
        }
    }
}
