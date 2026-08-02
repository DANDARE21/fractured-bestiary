package net.dandare21.fracturedutils.orchestrator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.S2CSyncSequenceTelemetryPacket;

import net.dandare21.fracturedutils.orchestrator.action.*;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrchestratorManager {
    private static final OrchestratorManager INSTANCE = new OrchestratorManager();
    private final List<SequenceInstance> activeRootSequences = new CopyOnWriteArrayList<>();
    private final Map<String, String> activeOperatorActions = new ConcurrentHashMap<>();

    public void registerOperatorAction(MinecraftServer server, String triggerId, String label) {
        if (triggerId == null || triggerId.isEmpty()) return;
        if (!activeOperatorActions.containsKey(triggerId)) {
            activeOperatorActions.put(triggerId, label != null ? label : "Resume Sequence");
            syncOperatorActionsToOps(server);
        }
    }

    public void unregisterOperatorAction(MinecraftServer server, String triggerId) {
        if (triggerId == null || triggerId.isEmpty()) return;
        if (activeOperatorActions.remove(triggerId) != null) {
            syncOperatorActionsToOps(server);
        }
    }

    public Map<String, String> getActiveOperatorActions() {
        return activeOperatorActions;
    }

    public void syncOperatorActionsToOps(MinecraftServer server) {
        if (server == null) return;
        net.dandare21.fracturedutils.network.packet.S2CSyncOperatorActionsPacket packet =
                new net.dandare21.fracturedutils.network.packet.S2CSyncOperatorActionsPacket(activeOperatorActions);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player != null && player.hasPermissions(2)) {
                ModMessages.sendToPlayer(packet, player);
            }
        }
    }

    private static final Gson GSON = ActionAdapter.registerAll(new GsonBuilder())
            .setPrettyPrinting()
            .create();

    public static OrchestratorManager getInstance() {
        return INSTANCE;
    }

    private OrchestratorManager() {
        ensureDirectoryExists();
    }

    public File getDirectory() {
        File dir = FMLPaths.CONFIGDIR.get().resolve("command_orchestrator").toFile();
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

    public SequenceInstance createSequenceInstance(String fileName, String targetPlayerName, SequenceInstance parent) {
        String cleanName = sanitizeFileName(fileName);
        File file = new File(getDirectory(), cleanName);

        if (!file.exists()) {
            FracturedUtils.LOGGER.error("Orchestrator file not found: {}", file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            List<OrchestratorAction> actions = GSON.fromJson(reader, new TypeToken<List<OrchestratorAction>>() {}.getType());
            if (actions == null) {
                actions = new ArrayList<>();
            }
            return new SequenceInstance(cleanName, targetPlayerName, actions, parent);
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("Failed to parse sequence file {}: {}", cleanName, e.getMessage());
            return null;
        }
    }

    public boolean startSequence(String fileName, String targetPlayerName) {
        SequenceInstance instance = createSequenceInstance(fileName, targetPlayerName, null);
        if (instance != null) {
            activeRootSequences.add(instance);
            return true;
        }
        return false;
    }

    public void tick(MinecraftServer server) {
        Iterator<SequenceInstance> iterator = activeRootSequences.iterator();
        while (iterator.hasNext()) {
            SequenceInstance sequence = iterator.next();
            sequence.tick(server);
            if (sequence.getState() == SequenceState.FINISHED) {
                activeRootSequences.remove(sequence);
            }
        }
        syncActiveSequenceTelemetryToOps(server);
    }

    public void syncActiveSequenceTelemetryToOps(MinecraftServer server) {
        if (server == null) return;
        List<S2CSyncSequenceTelemetryPacket.SequenceTelemetryData> telemetryList = new ArrayList<>();
        for (SequenceInstance seq : activeRootSequences) {
            List<S2CSyncSequenceTelemetryPacket.ActionInfo> actionInfos = new ArrayList<>();
            for (OrchestratorAction action : seq.getActions()) {
                String type = action.getType();
                String details = "";
                if (action instanceof CommandAction ca) {
                    details = ca.getRun();
                } else if (action instanceof WaitUntilAction wua) {
                    details = wua.getWaitType();
                } else if (action instanceof ForkSequenceAction fsa) {
                    details = fsa.getFile();
                } else if (action instanceof RunSequenceAction rsa) {
                    details = rsa.getFile();
                }
                actionInfos.add(new S2CSyncSequenceTelemetryPacket.ActionInfo(type, details));
            }
            telemetryList.add(new S2CSyncSequenceTelemetryPacket.SequenceTelemetryData(
                    seq.getSequenceName(),
                    seq.getTargetPlayerName(),
                    seq.getCurrentIndex(),
                    seq.getState().name(),
                    actionInfos
            ));
        }

        S2CSyncSequenceTelemetryPacket packet = new S2CSyncSequenceTelemetryPacket(telemetryList);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player != null && player.hasPermissions(2)) {
                ModMessages.sendToPlayer(packet, player);
            }
        }
    }

    public void onPlayerLoggedOut(ServerPlayer player) {
        String playerName = player.getScoreboardName();
        activeRootSequences.removeIf(seq -> seq.getTargetPlayerName() != null && seq.getTargetPlayerName().equalsIgnoreCase(playerName));
    }

    public boolean resumeTrigger(String playerName, String triggerId) {
        boolean resumed = false;
        for (SequenceInstance seq : activeRootSequences) {
            if (playerName == null || playerName.isEmpty() || seq.getTargetPlayerName() == null || seq.getTargetPlayerName().equalsIgnoreCase(playerName)) {
                if (seq.resumeTrigger(triggerId)) {
                    resumed = true;
                }
            }
        }
        return resumed;
    }

    public boolean resumeTrigger(String triggerId) {
        return resumeTrigger(null, triggerId);
    }

    private boolean matchesSequence(SequenceInstance seq, String playerName, String cleanFileName) {
        boolean playerMatches = (playerName == null || playerName.isEmpty() || seq.getTargetPlayerName() == null || seq.getTargetPlayerName().equalsIgnoreCase(playerName));
        boolean fileMatches = (cleanFileName == null || cleanFileName.isEmpty() || seq.getSequenceName().equalsIgnoreCase(cleanFileName));
        return playerMatches && fileMatches;
    }

    public boolean pauseSequence(String playerName, String fileName) {
        String cleanName = fileName != null && !fileName.isBlank() ? sanitizeFileName(fileName) : null;
        boolean pausedAny = false;
        for (SequenceInstance seq : activeRootSequences) {
            if (matchesSequence(seq, playerName, cleanName)) {
                seq.pause();
                pausedAny = true;
            }
        }
        return pausedAny;
    }

    public boolean cancelSequence(String playerName, String fileName) {
        String cleanName = fileName != null && !fileName.isBlank() ? sanitizeFileName(fileName) : null;
        boolean cancelledAny = false;
        Iterator<SequenceInstance> iterator = activeRootSequences.iterator();
        while (iterator.hasNext()) {
            SequenceInstance seq = iterator.next();
            if (matchesSequence(seq, playerName, cleanName)) {
                seq.cancel();
                iterator.remove();
                cancelledAny = true;
            }
        }
        return cancelledAny;
    }

    public List<String> getSequenceFileNames() {
        List<String> names = new ArrayList<>();
        File dir = getDirectory();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName());
                if (f.getName().endsWith(".json")) {
                    String withoutExt = f.getName().substring(0, f.getName().length() - 5);
                    if (!names.contains(withoutExt)) {
                        names.add(withoutExt);
                    }
                }
            }
        }
        return names;
    }

    public Map<String, String> getAllSequenceFiles() {
        Map<String, String> filesMap = new HashMap<>();
        File dir = getDirectory();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    String content = Files.readString(f.toPath());
                    filesMap.put(f.getName(), content);
                } catch (IOException e) {
                    FracturedUtils.LOGGER.error("Failed to read file {}", f.getName(), e);
                }
            }
        }
        return filesMap;
    }

    public boolean saveSequenceFile(String fileName, String jsonContent) {
        String cleanName = sanitizeFileName(fileName);
        if (cleanName.isEmpty()) {
            return false;
        }

        try {
            List<OrchestratorAction> actions = GSON.fromJson(jsonContent, new TypeToken<List<OrchestratorAction>>() {}.getType());
            if (actions == null) {
                return false;
            }
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("Invalid sequence JSON for file {}: {}", cleanName, e.getMessage(), e);
            return false;
        }

        File file = new File(getDirectory(), cleanName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonContent);
            return true;
        } catch (IOException e) {
            FracturedUtils.LOGGER.error("Failed to save sequence file {}", cleanName, e);
            return false;
        }
    }
}
