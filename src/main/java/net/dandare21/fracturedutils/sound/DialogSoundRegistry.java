package net.dandare21.fracturedutils.sound;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.client.gui.CyberpunkDropdown;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DialogSoundRegistry {

    private static final Gson GSON = new com.google.gson.GsonBuilder().setLenient().create();

    public static List<CyberpunkDropdown.DropdownEntry<String>> getAvailableSoundEntries(String currentSelectedValue) {
        List<CyberpunkDropdown.DropdownEntry<String>> entries = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();

        // 1. Silent Option
        entries.add(new CyberpunkDropdown.DropdownEntry<>("", Component.literal("Silent (No Letter Sound)")));
        addedKeys.add("");

        // 2. Built-in Preset Blips
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_default", "Default Blip (fractured_utils:dialog.blip_default)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_low", "Low Pitch Blip (fractured_utils:dialog.blip_low)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_high", "High Pitch Blip (fractured_utils:dialog.blip_high)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_sans", "Sans Voice (fractured_utils:dialog.blip_sans)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_papyrus", "Papyrus Voice (fractured_utils:dialog.blip_papyrus)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_robot", "Robot Voice (fractured_utils:dialog.blip_robot)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_typing", "Typewriter Key (fractured_utils:dialog.blip_typing)");
        addEntryIfNew(entries, addedKeys, "fractured_utils:dialog.blip_monster", "Monster Voice (fractured_utils:dialog.blip_monster)");

        // 3. Scan resourcepacks/ directory for custom sounds.json files
        scanResourcePacksDirectory(entries, addedKeys);

        // 4. Scan Minecraft SoundManager for all loaded resource pack sounds
        scanSoundManager(entries, addedKeys);

        // 5. Vanilla Presets
        addEntryIfNew(entries, addedKeys, "minecraft:block.dispenser.dispense", "Vanilla Dispenser Click");
        addEntryIfNew(entries, addedKeys, "minecraft:block.note_block.pling", "Vanilla Note Pling");
        addEntryIfNew(entries, addedKeys, "minecraft:entity.experience_orb.pickup", "Vanilla Experience Orb");
        addEntryIfNew(entries, addedKeys, "minecraft:gui.button.press", "Vanilla Button Press");

        // 6. Ensure current value is included if custom
        if (currentSelectedValue != null && !currentSelectedValue.trim().isEmpty()) {
            String val = currentSelectedValue.trim();
            if (!addedKeys.contains(val)) {
                addEntryIfNew(entries, addedKeys, val, "Custom Sound (" + val + ")");
            }
        }

        return entries;
    }

    private static void scanResourcePacksDirectory(List<CyberpunkDropdown.DropdownEntry<String>> entries, Set<String> addedKeys) {
        try {
            File resourcepacksDir = FMLPaths.GAMEDIR.get().resolve("resourcepacks").toFile();
            if (!resourcepacksDir.exists() || !resourcepacksDir.isDirectory()) return;

            File[] packs = resourcepacksDir.listFiles();
            if (packs == null) return;

            for (File pack : packs) {
                if (pack.isDirectory()) {
                    File assetsDir = new File(pack, "assets");
                    if (assetsDir.exists() && assetsDir.isDirectory()) {
                        File[] nsDirs = assetsDir.listFiles();
                        if (nsDirs != null) {
                            for (File nsDir : nsDirs) {
                                if (nsDir.isDirectory()) {
                                    String namespace = nsDir.getName().toLowerCase(Locale.ROOT);
                                    File soundsJson = new File(nsDir, "sounds.json");
                                    if (soundsJson.exists() && soundsJson.isFile()) {
                                        parseAndAddSoundsJson(soundsJson, namespace, entries, addedKeys);
                                    }
                                }
                            }
                        }
                    }
                } else if (pack.getName().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    try (ZipFile zip = new ZipFile(pack)) {
                        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
                        while (zipEntries.hasMoreElements()) {
                            ZipEntry zipEntry = zipEntries.nextElement();
                            String name = zipEntry.getName();
                            if (name.startsWith("assets/") && name.endsWith("/sounds.json")) {
                                String[] parts = name.split("/");
                                if (parts.length == 3) {
                                    String namespace = parts[1].toLowerCase(Locale.ROOT);
                                    try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(zipEntry))) {
                                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                                        if (json != null) {
                                            for (String key : json.keySet()) {
                                                String fullId = namespace + ":" + key;
                                                String label = formatSoundLabel(key, fullId);
                                                addEntryIfNew(entries, addedKeys, fullId, label);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("Failed scanning resourcepacks directory for custom sounds: {}", e.getMessage());
        }
    }

    private static void parseAndAddSoundsJson(File soundsJsonFile, String namespace, List<CyberpunkDropdown.DropdownEntry<String>> entries, Set<String> addedKeys) {
        try (FileReader reader = new FileReader(soundsJsonFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (String key : json.keySet()) {
                    String fullId = namespace + ":" + key;
                    String label = formatSoundLabel(key, fullId);
                    addEntryIfNew(entries, addedKeys, fullId, label);
                }
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("Failed parsing sounds.json at {}: {}", soundsJsonFile.getAbsolutePath(), e.getMessage());
        }
    }

    private static void scanSoundManager(List<CyberpunkDropdown.DropdownEntry<String>> entries, Set<String> addedKeys) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                Collection<ResourceLocation> available = mc.getSoundManager().getAvailableSounds();
                if (available != null) {
                    for (ResourceLocation loc : available) {
                        if (loc == null) continue;
                        String path = loc.getPath();
                        String ns = loc.getNamespace();
                        if (path != null && ns != null) {
                            if (path.startsWith("dialog.") || ns.contains("fractured")) {
                                String fullId = loc.toString();
                                String label = formatSoundLabel(path, fullId);
                                addEntryIfNew(entries, addedKeys, fullId, label);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String formatSoundLabel(String soundKey, String fullId) {
        String cleanName = soundKey;
        if (cleanName.startsWith("dialog.")) {
            cleanName = cleanName.substring(7);
        }
        String readableName = capitalizeWords(cleanName.replace('_', ' '));
        return readableName + " (" + fullId + ")";
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return "";
        String[] words = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static void addEntryIfNew(List<CyberpunkDropdown.DropdownEntry<String>> entries, Set<String> addedKeys, String value, String label) {
        if (value == null) return;
        String key = value.trim();
        if (!addedKeys.contains(key)) {
            addedKeys.add(key);
            entries.add(new CyberpunkDropdown.DropdownEntry<>(key, Component.literal(label)));
        }
    }
}
