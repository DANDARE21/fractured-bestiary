package net.dandare21.fracturedutils.sound;

import net.minecraft.sounds.SoundSource;

public class ModSoundSources {

    // Scaled with Master Sound so vanilla RECORDS/AMBIENT sliders never mute custom event music
    public static final SoundSource EVENT_MUSIC = SoundSource.MASTER;
    public static final SoundSource EVENT_AMBIENCE = SoundSource.MASTER;

    public static SoundSource parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) return EVENT_MUSIC;
        String s = categoryStr.trim().toLowerCase();
        if (s.contains("ambien")) return EVENT_AMBIENCE;
        if (s.contains("music")) return EVENT_MUSIC;
        if (s.contains("record")) return EVENT_MUSIC;
        if (s.contains("master")) return SoundSource.MASTER;
        for (SoundSource source : SoundSource.values()) {
            if (source.getName().equalsIgnoreCase(s)) {
                return source;
            }
        }
        return EVENT_MUSIC;
    }
}
