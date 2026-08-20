package net.dandare21.fracturedutils.sound.sequence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.sound.ModSoundSources;
import net.dandare21.fracturedutils.sound.event.EventAudioManager;
import net.dandare21.fracturedutils.network.packet.S2CPlayEventAudioPacket.PlaybackMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MusicSequenceManager {
    private static final MusicSequenceManager INSTANCE = new MusicSequenceManager();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static class ActiveMusicSequence {
        private final String fileName;
        private final MusicSequence sequence;
        private final long startTimeMs;
        private final long expectedDurationMs;
        private final Set<UUID> targetPlayerUuids;
        private final Set<Integer> executedEntryIndices = new HashSet<>();
        private boolean finished = false;

        public ActiveMusicSequence(String fileName, MusicSequence sequence, Collection<ServerPlayer> targets) {
            this.fileName = fileName;
            this.sequence = sequence;
            this.startTimeMs = System.currentTimeMillis();
            this.targetPlayerUuids = new HashSet<>();
            if (targets != null) {
                for (ServerPlayer player : targets) {
                    if (player != null) {
                        targetPlayerUuids.add(player.getUUID());
                    }
                }
            }

            long songDuration = 0L;
            if (sequence.getSongTrack() != null && !sequence.getSongTrack().trim().isEmpty()) {
                byte[] oggBytes = net.dandare21.fracturedutils.sound.event.AudioTrackBytesProvider.getTrackBytes(sequence.getSongTrack());
                songDuration = decodeOggDurationMs(oggBytes);
            }

            long maxEntryTimestamp = 0L;
            for (MusicSequenceEntry entry : sequence.getEntries()) {
                if (entry.getTimestampMs() > maxEntryTimestamp) {
                    maxEntryTimestamp = entry.getTimestampMs();
                }
            }

            if (sequence.getEndMs() > 0) {
                this.expectedDurationMs = sequence.getEndMs();
            } else if (songDuration > 0) {
                this.expectedDurationMs = songDuration;
            } else {
                this.expectedDurationMs = Math.max(30000L, maxEntryTimestamp + 1000L);
            }
        }

        public String getFileName() {
            return fileName;
        }

        public MusicSequence getSequence() {
            return sequence;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public long getExpectedDurationMs() {
            return expectedDurationMs;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    public static long decodeOggDurationMs(byte[] bytes) {
        if (bytes == null || bytes.length < 28) return 0L;

        int sampleRate = 0;
        for (int i = 0; i <= bytes.length - 15; i++) {
            if (bytes[i] == 1 && bytes[i + 1] == 'v' && bytes[i + 2] == 'o' && bytes[i + 3] == 'r'
                    && bytes[i + 4] == 'b' && bytes[i + 5] == 'i' && bytes[i + 6] == 's') {
                sampleRate = (bytes[i + 11] & 0xFF) |
                        ((bytes[i + 12] & 0xFF) << 8) |
                        ((bytes[i + 13] & 0xFF) << 16) |
                        ((bytes[i + 14] & 0xFF) << 24);
                break;
            }
        }

        if (sampleRate <= 0) return 0L;

        long totalSamples = -1;
        for (int i = bytes.length - 4; i >= 0; i--) {
            if (bytes[i] == 0x4F && bytes[i + 1] == 0x67 && bytes[i + 2] == 0x67 && bytes[i + 3] == 0x53) {
                if (i + 13 < bytes.length) {
                    long granule = (bytes[i + 6] & 0xFFL) |
                            ((bytes[i + 7] & 0xFFL) << 8) |
                            ((bytes[i + 8] & 0xFFL) << 16) |
                            ((bytes[i + 9] & 0xFFL) << 24) |
                            ((bytes[i + 10] & 0xFFL) << 32) |
                            ((bytes[i + 11] & 0xFFL) << 40) |
                            ((bytes[i + 12] & 0xFFL) << 48) |
                            ((bytes[i + 13] & 0xFFL) << 56);
                    if (granule > 0) {
                        totalSamples = granule;
                        break;
                    }
                }
            }
        }

        if (totalSamples > 0) {
            return (totalSamples * 1000L) / sampleRate;
        }

        return 0L;
    }

    private final List<ActiveMusicSequence> activeSequences = new CopyOnWriteArrayList<>();

    public static MusicSequenceManager getInstance() {
        return INSTANCE;
    }

    public boolean isSequenceActive(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String cleanName = sanitizeFileName(fileName);
        for (ActiveMusicSequence activeSeq : activeSequences) {
            String activeClean = sanitizeFileName(activeSeq.getFileName());
            if (activeClean.equalsIgnoreCase(cleanName) && !activeSeq.isFinished()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActiveSequences() {
        return !activeSequences.isEmpty();
    }

    private MusicSequenceManager() {
        ensureDirectoryExists();
    }

    public File getDirectory() {
        File dir = FMLPaths.CONFIGDIR.get().resolve("music_sequences").toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public void ensureDirectoryExists() {
        getDirectory();
    }

    public String sanitizeFileName(String fileName) {
        if (fileName == null) return "";
        fileName = fileName.trim();
        fileName = fileName.replaceAll("[\\\\/]", "");
        if (!fileName.endsWith(".json")) {
            fileName += ".json";
        }
        return fileName;
    }

    public List<String> getSequenceFileNames() {
        File dir = getDirectory();
        if (!dir.exists()) return Collections.emptyList();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (File f : files) {
            list.add(f.getName());
        }
        Collections.sort(list);
        return list;
    }

    public Map<String, String> getAllSequenceFiles() {
        Map<String, String> map = new HashMap<>();
        File dir = getDirectory();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    String content = Files.readString(f.toPath());
                    map.put(f.getName(), content);
                } catch (IOException e) {
                    FracturedUtils.LOGGER.error("[MusicSequenceManager] Failed to read music sequence file {}: {}", f.getName(), e.getMessage());
                }
            }
        }
        return map;
    }

    public boolean saveSequenceFile(String fileName, String jsonContent) {
        String cleanName = sanitizeFileName(fileName);
        if (cleanName.isEmpty()) return false;

        try {
            MusicSequence sequence = GSON.fromJson(jsonContent, MusicSequence.class);
            if (sequence == null) {
                sequence = new MusicSequence();
            }
            sequence.sortEntriesByTimestamp();
            String formattedJson = GSON.toJson(sequence);

            File file = new File(getDirectory(), cleanName);
            Files.writeString(file.toPath(), formattedJson);
            FracturedUtils.LOGGER.info("[MusicSequenceManager] Saved music sequence file: {}", cleanName);
            return true;
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[MusicSequenceManager] Error saving music sequence file {}: {}", cleanName, e.getMessage());
            return false;
        }
    }

    public boolean deleteSequenceFile(String fileName) {
        String cleanName = sanitizeFileName(fileName);
        if (cleanName.isEmpty()) return false;

        File file = new File(getDirectory(), cleanName);
        if (file.exists() && file.delete()) {
            FracturedUtils.LOGGER.info("[MusicSequenceManager] Deleted music sequence file: {}", cleanName);
            return true;
        }
        return false;
    }

    public MusicSequence loadSequence(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return null;
        File file = new File(getDirectory(), sanitizeFileName(fileName));
        if (!file.exists()) return null;

        try {
            String json = Files.readString(file.toPath());
            return GSON.fromJson(json, MusicSequence.class);
        } catch (IOException e) {
            FracturedUtils.LOGGER.error("[MusicSequenceManager] Failed to load music sequence '{}'", fileName, e);
            return null;
        }
    }

    public boolean saveSequence(String fileName, MusicSequence sequence) {
        if (fileName == null || fileName.trim().isEmpty() || sequence == null) return false;
        File file = new File(getDirectory(), sanitizeFileName(fileName));

        try {
            String json = GSON.toJson(sequence);
            Files.writeString(file.toPath(), json);
            FracturedUtils.LOGGER.info("[MusicSequenceManager] Saved music sequence '{}'", fileName);
            return true;
        } catch (IOException e) {
            FracturedUtils.LOGGER.error("[MusicSequenceManager] Failed to save music sequence '{}'", fileName, e);
            return false;
        }
    }

    public boolean startSequence(String fileName, Collection<ServerPlayer> targets) {
        MusicSequence sequence = loadSequence(fileName);
        if (sequence == null) return false;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();

        // 1. Start Audio Playback via EventAudioManager if song track is specified
        if (sequence.getSongTrack() != null && !sequence.getSongTrack().trim().isEmpty()) {
            EventAudioManager.getInstance().playAudio(
                    server,
                    sequence.getSongTrack(),
                    ModSoundSources.EVENT_MUSIC,
                    targets,
                    sequence.getVolume(),
                    sequence.getPitch(),
                    1000,
                    PlaybackMode.SERVER_CONTROLLED,
                    sequence.isLooping(),
                    2000
            );
        }

        // 2. Track Active Sequence for Timed Action Execution
        ActiveMusicSequence activeSeq = new ActiveMusicSequence(fileName, sequence, targets);
        activeSequences.add(activeSeq);
        FracturedUtils.LOGGER.info("[MusicSequenceManager] Started music sequence '{}' with {} entries (expected duration: {}ms).", fileName, sequence.getEntries().size(), activeSeq.getExpectedDurationMs());
        return true;
    }

    public void stopAllSequences(MinecraftServer server) {
        activeSequences.clear();
        EventAudioManager.getInstance().stopAudio(server, null, 1000);
        FracturedUtils.LOGGER.info("[MusicSequenceManager] Stopped all active music sequences.");
    }

    public void tick(MinecraftServer server) {
        if (server == null || activeSequences.isEmpty()) return;

        long now = System.currentTimeMillis();

        for (ActiveMusicSequence activeSeq : activeSequences) {
            long elapsedMs = now - activeSeq.getStartTimeMs();
            List<MusicSequenceEntry> entries = activeSeq.getSequence().getEntries();

            for (int i = 0; i < entries.size(); i++) {
                if (activeSeq.executedEntryIndices.contains(i)) {
                    continue;
                }

                MusicSequenceEntry entry = entries.get(i);
                if (elapsedMs >= entry.getTimestampMs()) {
                    executeEntry(server, activeSeq, entry);
                    activeSeq.executedEntryIndices.add(i);
                }
            }

            // Mark finished ONLY when non-looping AND song track playback has completed (elapsedMs >= expectedDurationMs)
            if (!activeSeq.getSequence().isLooping()) {
                if (elapsedMs >= activeSeq.getExpectedDurationMs()) {
                    activeSeq.finished = true;
                }
            }
        }

        activeSequences.removeIf(ActiveMusicSequence::isFinished);
    }

    private void executeEntry(MinecraftServer server, ActiveMusicSequence activeSeq, MusicSequenceEntry entry) {
        if (entry.getCommand() == null || entry.getCommand().trim().isEmpty()) return;

        String cmd = entry.getCommand().trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        CommandSourceStack sourceStack = server.createCommandSourceStack();

        try {
            server.getCommands().performPrefixedCommand(sourceStack, cmd);
            FracturedUtils.LOGGER.info("[MusicSequenceManager] Executed entry command at {}ms: '{}'", entry.getTimestampMs(), cmd);
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[MusicSequenceManager] Error executing entry command '{}' in sequence {}", cmd, activeSeq.getFileName(), e);
        }
    }

    public List<ActiveMusicSequence> getActiveSequences() {
        return activeSequences;
    }
}
