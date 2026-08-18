package net.dandare21.fracturedutils.sound.util;

import net.dandare21.fracturedutils.sound.event.ClientAudioConfig;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SmoothFadeSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final String customChannel; // "eventmusic" or "eventambience"
    private boolean stopped = false;

    private float baseTargetVolume = 1.0f;
    private float startVolume = 1.0f;
    private float currentVolume = 1.0f;
    private boolean isFading = false;
    private int fadeDurationMs = 0;
    private long fadeStartTimeMs = 0;
    private boolean stopOnFadeOutComplete = false;

    public SmoothFadeSoundInstance(SoundEvent soundEvent, SoundSource category, String customChannel, float volume, float pitch, boolean looping) {
        super(soundEvent.getLocation(), category, RandomSource.create());
        this.customChannel = customChannel != null ? customChannel : "eventmusic";
        this.baseTargetVolume = volume;
        this.currentVolume = Math.max(0.01f, volume);
        this.volume = Math.max(0.01f, volume);
        this.pitch = pitch;
        this.looping = looping;
        this.delay = 0;
        this.relative = true;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    public void startFadeIn(float targetVol, int fadeDurationMs) {
        this.baseTargetVolume = targetVol;
        this.stopOnFadeOutComplete = false;

        if (fadeDurationMs <= 0) {
            this.currentVolume = targetVol;
            this.volume = targetVol;
            this.isFading = false;
        } else {
            this.startVolume = 0.001f;
            this.currentVolume = 0.001f;
            this.volume = 0.001f;
            this.fadeDurationMs = fadeDurationMs;
            this.fadeStartTimeMs = System.currentTimeMillis();
            this.isFading = true;
        }
    }

    public void startFadeOutAndStop(int fadeDurationMs) {
        this.stopOnFadeOutComplete = true;

        if (fadeDurationMs <= 0) {
            this.currentVolume = 0.0f;
            this.volume = 0.0f;
            this.stopped = true;
            this.isFading = false;
        } else {
            this.startVolume = Math.max(0.001f, this.currentVolume);
            this.baseTargetVolume = 0.0f;
            this.fadeDurationMs = fadeDurationMs;
            this.fadeStartTimeMs = System.currentTimeMillis();
            this.isFading = true;
        }
    }

    public void stopSound() {
        this.stopped = true;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    @Override
    public void tick() {
        if (stopped) return;

        if (isFading && fadeDurationMs > 0) {
            long elapsed = System.currentTimeMillis() - fadeStartTimeMs;
            float progress = Math.min(1.0f, (float) elapsed / fadeDurationMs);
            this.currentVolume = startVolume + (baseTargetVolume - startVolume) * progress;
            this.volume = currentVolume;

            if (progress >= 1.0f) {
                this.isFading = false;
                if (stopOnFadeOutComplete) {
                    this.stopped = true;
                }
            }
        }
    }

    @Override
    public float getVolume() {
        float sliderVolume = ClientAudioConfig.getChannelVolume(this.customChannel);
        float result = this.volume * sliderVolume;
        return Math.max(0.001f, result); // Ensure Minecraft SoundEngine never discards active sound instance as 0.0f silent!
    }

    public String getCustomChannel() {
        return customChannel;
    }
}
