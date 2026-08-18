package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.ModSoundSources;
import net.dandare21.fracturedutils.sound.event.EventAudioClientController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CPlayEventAudioPacket {

    public enum PlaybackMode {
        FIRE_AND_FORGET,
        SERVER_CONTROLLED
    }

    private final String soundEventId;
    private final SoundSource category;
    private final float volume;
    private final float pitch;
    private final int fadeDurationMs;
    private final long startOffsetMs;
    private final boolean stopPrevious;
    private final PlaybackMode mode;
    private final boolean looping;
    private final int syncThresholdMs;

    public S2CPlayEventAudioPacket(String soundEventId, SoundSource category, float volume, float pitch, int fadeDurationMs, long startOffsetMs, boolean stopPrevious, PlaybackMode mode, boolean looping, int syncThresholdMs) {
        this.soundEventId = soundEventId != null ? soundEventId : "";
        this.category = category != null ? category : ModSoundSources.EVENT_MUSIC;
        this.volume = volume;
        this.pitch = pitch;
        this.fadeDurationMs = fadeDurationMs;
        this.startOffsetMs = startOffsetMs;
        this.stopPrevious = stopPrevious;
        this.mode = mode != null ? mode : PlaybackMode.SERVER_CONTROLLED;
        this.looping = looping;
        this.syncThresholdMs = syncThresholdMs;
    }

    public S2CPlayEventAudioPacket(FriendlyByteBuf buf) {
        this.soundEventId = buf.readUtf();
        String categoryName = buf.readUtf();
        this.category = ModSoundSources.parseCategory(categoryName);
        this.volume = buf.readFloat();
        this.pitch = buf.readFloat();
        this.fadeDurationMs = buf.readVarInt();
        this.startOffsetMs = buf.readVarLong();
        this.stopPrevious = buf.readBoolean();
        this.mode = buf.readEnum(PlaybackMode.class);
        this.looping = buf.readBoolean();
        this.syncThresholdMs = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.soundEventId);
        buf.writeUtf(this.category != null ? this.category.getName() : "eventmusic");
        buf.writeFloat(this.volume);
        buf.writeFloat(this.pitch);
        buf.writeVarInt(this.fadeDurationMs);
        buf.writeVarLong(this.startOffsetMs);
        buf.writeBoolean(this.stopPrevious);
        buf.writeEnum(this.mode);
        buf.writeBoolean(this.looping);
        buf.writeVarInt(this.syncThresholdMs);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                EventAudioClientController.getInstance().playAudio(
                        soundEventId, category, volume, pitch, fadeDurationMs, startOffsetMs, stopPrevious, mode, looping, syncThresholdMs
                );
            });
        });
        return true;
    }

    public String getSoundEventId() { return soundEventId; }
    public SoundSource getCategory() { return category; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public int getFadeDurationMs() { return fadeDurationMs; }
    public long getStartOffsetMs() { return startOffsetMs; }
    public boolean isStopPrevious() { return stopPrevious; }
    public PlaybackMode getMode() { return mode; }
    public boolean isLooping() { return looping; }
    public int getSyncThresholdMs() { return syncThresholdMs; }
}
