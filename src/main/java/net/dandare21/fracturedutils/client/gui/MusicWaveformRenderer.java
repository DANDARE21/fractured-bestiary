package net.dandare21.fracturedutils.client.gui;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.sound.event.ClientAudioPackManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MusicWaveformRenderer {

    public static class TrackWaveformData {
        public static final TrackWaveformData EMPTY = new TrackWaveformData(new float[0], 0L);

        public final float[] amplitudes;
        public final long totalDurationMs;

        public TrackWaveformData(float[] amplitudes, long totalDurationMs) {
            this.amplitudes = amplitudes;
            this.totalDurationMs = totalDurationMs;
        }
    }

    private static final Map<String, TrackWaveformData> WAVEFORM_CACHE = Collections.synchronizedMap(new HashMap<>());
    private static final Set<String> LOADING_TRACKS = Collections.synchronizedSet(new HashSet<>());

    public static TrackWaveformData getOrComputeTrueWaveform(String soundTrack) {
        if (soundTrack == null || soundTrack.trim().isEmpty()) {
            return null;
        }

        String cleanTrack = soundTrack.trim();

        if (WAVEFORM_CACHE.containsKey(cleanTrack)) {
            return WAVEFORM_CACHE.get(cleanTrack);
        }

        if (!LOADING_TRACKS.contains(cleanTrack)) {
            LOADING_TRACKS.add(cleanTrack);

            CompletableFuture.runAsync(() -> {
                try {
                    TrackWaveformData decoded = decodeTruePcmWaveform(cleanTrack);
                    if (decoded != null) {
                        WAVEFORM_CACHE.put(cleanTrack, decoded);
                        FracturedUtils.LOGGER.info("[MusicWaveformRenderer] Successfully decoded true PCM waveform for track '{}' ({}ms, {} slices)", cleanTrack, decoded.totalDurationMs, decoded.amplitudes.length);
                    } else {
                        WAVEFORM_CACHE.put(cleanTrack, TrackWaveformData.EMPTY);
                        FracturedUtils.LOGGER.warn("[MusicWaveformRenderer] Could not decode track bytes for '{}', cached empty fallback.", cleanTrack);
                    }
                } catch (Exception e) {
                    WAVEFORM_CACHE.put(cleanTrack, TrackWaveformData.EMPTY);
                    FracturedUtils.LOGGER.error("[MusicWaveformRenderer] Error decoding PCM waveform for track '{}'", cleanTrack, e);
                } finally {
                    LOADING_TRACKS.remove(cleanTrack);
                }
            });
        }

        return null;
    }

    private static TrackWaveformData decodeTruePcmWaveform(String soundTrack) {
        byte[] oggBytes = ClientAudioPackManager.getInstance().getTrackBytes(soundTrack);
        if (oggBytes == null || oggBytes.length == 0) {
            return null;
        }

        ByteBuffer rawOggBuffer = null;
        long decoder = 0;
        try {
            rawOggBuffer = MemoryUtil.memAlloc(oggBytes.length);
            rawOggBuffer.put(oggBytes);
            rawOggBuffer.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                decoder = STBVorbis.stb_vorbis_open_memory(rawOggBuffer, error, null);
                if (decoder == 0) {
                    return null;
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(decoder, info);
                int channels = Math.max(1, info.channels());
                int sampleRate = Math.max(1, info.sample_rate());
                int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);

                if (totalSamples <= 0) {
                    return null;
                }

                long totalDurationMs = (long) (((double) totalSamples / sampleRate) * 1000.0);

                int numSlices = 600;
                float[] amplitudes = new float[numSlices];
                int samplesPerSlice = Math.max(1, totalSamples / numSlices);

                ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(4096 * channels);

                int currentSlice = 0;
                int samplesAccumulatedInSlice = 0;
                float maxAmpInSlice = 0.0f;

                while (currentSlice < numSlices) {
                    pcmBuffer.clear();
                    int samplesRead = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcmBuffer);
                    if (samplesRead <= 0) break;

                    for (int i = 0; i < samplesRead; i++) {
                        for (int c = 0; c < channels; c++) {
                            short sample = pcmBuffer.get();
                            float amp = Math.abs(sample) / 32768.0f;
                            if (amp > maxAmpInSlice) {
                                maxAmpInSlice = amp;
                            }
                        }
                        samplesAccumulatedInSlice++;

                        if (samplesAccumulatedInSlice >= samplesPerSlice) {
                            amplitudes[currentSlice] = Math.min(1.0f, Math.max(0.02f, maxAmpInSlice));
                            currentSlice++;
                            samplesAccumulatedInSlice = 0;
                            maxAmpInSlice = 0.0f;
                            if (currentSlice >= numSlices) break;
                        }
                    }
                }

                return new TrackWaveformData(amplitudes, totalDurationMs);
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[MusicWaveformRenderer] Vorbis decode failure for {}", soundTrack, e);
            return null;
        } finally {
            if (decoder != 0) {
                STBVorbis.stb_vorbis_close(decoder);
            }
            if (rawOggBuffer != null) {
                MemoryUtil.memFree(rawOggBuffer);
            }
        }
    }

    public static void renderWaveform(GuiGraphics guiGraphics, String songTrack, int startX, int startY, int trackWidth, int trackHeight, double timeScrollMs, double pixelsPerSecond) {
        Font font = Minecraft.getInstance().font;

        if (songTrack == null || songTrack.trim().isEmpty()) {
            guiGraphics.drawString(font, "CH 0: AUDIO WAVEFORM [No Song Selected]", startX + 8, startY + 6, 0xFF00E5FF, false);
            return;
        }

        TrackWaveformData waveformData = getOrComputeTrueWaveform(songTrack);

        if (waveformData == null) {
            // Render Cyberpunk Loading Indicator
            long time = System.currentTimeMillis();
            String[] spinner = new String[]{"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
            String symbol = spinner[(int) ((time / 80) % spinner.length)];

            guiGraphics.drawString(font, "CH 0: AUDIO WAVEFORM (" + songTrack + ") " + symbol + " DECODING TRUE PCM WAVEFORM...", startX + 8, startY + 6, 0xFFFFD700, false);

            // Animated loading bar accent
            int animX = (int) ((time / 10) % (trackWidth - 80));
            guiGraphics.fill(startX + animX, startY + trackHeight - 4, startX + animX + 80, startY + trackHeight - 2, 0xFFFFD700);
            return;
        }

        float[] amplitudes = waveformData.amplitudes;
        long duration = waveformData.totalDurationMs > 0 ? waveformData.totalDurationMs : 180000L;

        if (amplitudes == null || amplitudes.length == 0) {
            guiGraphics.drawString(font, "CH 0: AUDIO WAVEFORM (" + songTrack + " - File Not Found / Unreadable)", startX + 8, startY + 6, 0xFFFF3355, false);
            int centerY = startY + (trackHeight / 2) + 4;
            guiGraphics.fill(startX, centerY, startX + trackWidth, centerY + 1, 0xAAFF3355);
            return;
        }

        // Render Track Header Tag with True Duration
        long durSec = duration / 1000;
        String durationStr = String.format("%02d:%02d.%03d", durSec / 60, durSec % 60, duration % 1000);
        guiGraphics.drawString(font, "CH 0: AUDIO WAVEFORM (" + songTrack + " - " + durationStr + ")", startX + 8, startY + 6, 0xFF00E5FF, false);

        int centerY = startY + (trackHeight / 2) + 4;
        int maxAmpHeight = (trackHeight / 2) - 8;

        // Render True PCM Waveform
        for (int x = 0; x < trackWidth; x++) {
            double currentMs = timeScrollMs + ((double) x / pixelsPerSecond) * 1000.0;
            if (currentMs < 0 || currentMs > duration) continue;

            double ratio = currentMs / duration;
            int ampIndex = (int) Math.min(amplitudes.length - 1, Math.max(0, ratio * amplitudes.length));
            float amp = amplitudes[ampIndex];

            int barHeight = (int) (amp * maxAmpHeight);

            int barX = startX + x;
            int topY = centerY - barHeight;
            int bottomY = centerY + barHeight;

            // True waveform vertical bars
            guiGraphics.fill(barX, topY, barX + 1, bottomY, 0xDD00E5FF);
            guiGraphics.fill(barX, topY, barX + 1, topY + 1, 0xFFFFFFFF);
            guiGraphics.fill(barX, bottomY - 1, barX + 1, bottomY, 0xFFFFFFFF);
        }

        // Center Zero Line
        guiGraphics.fill(startX, centerY, startX + trackWidth, centerY + 1, 0xAA00E5FF);
    }
}
