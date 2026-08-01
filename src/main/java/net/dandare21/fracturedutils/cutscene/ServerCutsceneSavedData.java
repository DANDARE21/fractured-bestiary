package net.dandare21.fracturedutils.cutscene;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

public class ServerCutsceneSavedData extends SavedData {
    private static final String DATA_NAME = "fractured_utils_cutscenes";
    private final Map<String, String> namedCinematics = new HashMap<>();

    public static ServerCutsceneSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(ServerCutsceneSavedData::load, ServerCutsceneSavedData::new, DATA_NAME);
    }

    public static ServerCutsceneSavedData load(CompoundTag tag) {
        ServerCutsceneSavedData data = new ServerCutsceneSavedData();
        CompoundTag mapTag = tag.getCompound("NamedCinematics");
        for (String key : mapTag.getAllKeys()) {
            data.namedCinematics.put(key.toLowerCase(), mapTag.getString(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<String, String> entry : namedCinematics.entrySet()) {
            mapTag.putString(entry.getKey().toLowerCase(), entry.getValue());
        }
        tag.put("NamedCinematics", mapTag);
        return tag;
    }

    public void registerCutscene(String name, String url) {
        if (name != null && url != null) {
            namedCinematics.put(name.toLowerCase(), url);
            setDirty();
        }
    }

    public String getUrl(String name) {
        if (name == null) return null;
        return namedCinematics.get(name.toLowerCase());
    }

    public Map<String, String> getNamedCinematics() {
        return namedCinematics;
    }
}
