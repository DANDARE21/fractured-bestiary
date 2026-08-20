package net.dandare21.fracturedutils.orchestrator.action;

import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class PlayMusicSequenceAction implements OrchestratorAction {
    private String type = "play_music_sequence";
    private String sequenceFile;
    private boolean awaitCompletion;

    public PlayMusicSequenceAction() {
        this.sequenceFile = "";
        this.awaitCompletion = false;
    }

    public PlayMusicSequenceAction(String sequenceFile) {
        this(sequenceFile, false);
    }

    public PlayMusicSequenceAction(String sequenceFile, boolean awaitCompletion) {
        this.sequenceFile = sequenceFile != null ? sequenceFile : "";
        this.awaitCompletion = awaitCompletion;
    }

    public String getSequenceFile() {
        return sequenceFile;
    }

    public void setSequenceFile(String sequenceFile) {
        this.sequenceFile = sequenceFile != null ? sequenceFile : "";
    }

    public boolean isAwaitCompletion() {
        return awaitCompletion;
    }

    public void setAwaitCompletion(boolean awaitCompletion) {
        this.awaitCompletion = awaitCompletion;
    }

    private transient boolean startedInThisPass = false;

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (server == null || sequenceFile == null || sequenceFile.trim().isEmpty()) {
            return ActionResult.SUCCESS;
        }

        List<ServerPlayer> targetPlayers = server.getPlayerList().getPlayers();
        if (targetPlayers.isEmpty()) {
            return ActionResult.SUCCESS;
        }

        MusicSequenceManager mgr = MusicSequenceManager.getInstance();

        if (!startedInThisPass) {
            if (!mgr.isSequenceActive(sequenceFile)) {
                mgr.startSequence(sequenceFile, targetPlayers);
            }
            startedInThisPass = true;
        }

        if (awaitCompletion) {
            if (mgr.isSequenceActive(sequenceFile)) {
                return ActionResult.BLOCK;
            }
        }

        startedInThisPass = false;
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "play_music_sequence";
    }

    @Override
    public OrchestratorAction copy() {
        return new PlayMusicSequenceAction(this.sequenceFile, this.awaitCompletion);
    }
}
