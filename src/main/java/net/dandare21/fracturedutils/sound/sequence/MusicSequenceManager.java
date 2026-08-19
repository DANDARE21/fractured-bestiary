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

        public boolean isFinished() {
            return finished;
        }
    }

    private final List<ActiveMusicSequence> activeSequences = new CopyOnWriteArrayList<>();

    public static MusicSequenceManager getInstance() {
        return INSTANCE;
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
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        List<String> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                list.add(f.getName());
            }
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
        String cleanName = sanitizeFileName(fileName);
        File file = new File(getDirectory(), cleanName);
        if (!file.exists()) return null;

        try {
            String json = Files.readString(file.toPath());
            MusicSequence seq = GSON.fromJson(json, MusicSequence.class);
            if (seq != null) {
                seq.sortEntriesByTimestamp();
            }
            return seq;
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[MusicSequenceManager] Failed to parse music sequence file {}: {}", cleanName, e.getMessage());
            return null;
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
        FracturedUtils.LOGGER.info("[MusicSequenceManager] Started music sequence '{}' with {} entries.", fileName, sequence.getEntries().size());
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

            boolean allExecuted = true;
            for (int i = 0; i < entries.size(); i++) {
                if (activeSeq.executedEntryIndices.contains(i)) {
                    continue;
                }

                MusicSequenceEntry entry = entries.get(i);
                if (elapsedMs >= entry.getTimestampMs()) {
                    executeEntry(server, activeSeq, entry);
                    activeSeq.executedEntryIndices.add(i);
                } else {
                    allExecuted = false;
                }
            }

            // Mark finished if non-looping and all entries executed (and music has finished or time exceeds max entry + buffer)
            if (allExecuted && !activeSeq.getSequence().isLooping()) {
                long maxTimestamp = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).getTimestampMs();
                if (elapsedMs > maxTimestamp + 5000L) {
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
