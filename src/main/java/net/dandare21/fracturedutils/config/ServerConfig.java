package net.dandare21.fracturedutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("fracturedutils-server.json");

    private static boolean keepInventoryNoXp = false;
    private static int teamWipeScreenDurationSeconds = 3;
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        if (json.has("keepInventoryNoXp")) {
                            keepInventoryNoXp = json.get("keepInventoryNoXp").getAsBoolean();
                        }
                        if (json.has("teamWipeScreenDurationSeconds")) {
                            teamWipeScreenDurationSeconds = Math.max(1, json.get("teamWipeScreenDurationSeconds").getAsInt());
                        }
                        FracturedUtils.LOGGER.info("[ServerConfig] Loaded server configuration.");
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[ServerConfig] Failed to load server config", e);
        } finally {
            loaded = true;
        }
    }

    public static synchronized void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("keepInventoryNoXp", keepInventoryNoXp);
            json.addProperty("teamWipeScreenDurationSeconds", teamWipeScreenDurationSeconds);

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.warn("[ServerConfig] Failed to save server config", e);
        }
    }

    public static boolean isKeepInventoryNoXpEnabled() {
        load();
        return keepInventoryNoXp;
    }

    public static void setKeepInventoryNoXpEnabled(boolean enabled) {
        load();
        if (keepInventoryNoXp != enabled) {
            keepInventoryNoXp = enabled;
            save();
        }
    }

    public static int getTeamWipeScreenDurationSeconds() {
        load();
        return teamWipeScreenDurationSeconds;
    }

    public static void setTeamWipeScreenDurationSeconds(int seconds) {
        load();
        int val = Math.max(1, seconds);
        if (teamWipeScreenDurationSeconds != val) {
            teamWipeScreenDurationSeconds = val;
            save();
        }
    }
}
