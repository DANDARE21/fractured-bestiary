package net.dandare21.fracturedutils.client;

public class ClientCutsceneConfig {
    public static void load() {
        ClientConfig.load();
    }

    public static void save() {
        ClientConfig.save();
    }

    public static int getVideoVolumePercent() {
        return ClientConfig.getVideoVolumePercent();
    }

    public static void setVideoVolumePercent(int percent) {
        ClientConfig.setVideoVolumePercent(percent);
    }
}
