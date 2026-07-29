package net.dandare21.fracturedutils.maintenance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class MaintenanceSavedData extends SavedData {
    public static final String FILE_NAME = "fractured_utils_maintenance";

    private boolean active = false;
    private String reason = "Server is currently under maintenance.";

    public MaintenanceSavedData() {
    }

    public static MaintenanceSavedData load(CompoundTag nbt) {
        MaintenanceSavedData data = new MaintenanceSavedData();
        data.active = nbt.getBoolean("active");
        if (nbt.contains("reason")) {
            data.reason = nbt.getString("reason");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("active", this.active);
        nbt.putString("reason", this.reason != null ? this.reason : "");
        return nbt;
    }

    public static MaintenanceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MaintenanceSavedData::load,
                MaintenanceSavedData::new,
                FILE_NAME
        );
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setDirty();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        setDirty();
    }
}
