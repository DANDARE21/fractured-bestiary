package net.dandare21.fracturedutils.sound.event;

import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.C2SResourcePackStatusPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@OnlyIn(Dist.CLIENT)
public class ClientAudioPackManager {

    private static final ClientAudioPackManager INSTANCE = new ClientAudioPackManager();

    private final EventMusicPackBuilder packBuilder = new EventMusicPackBuilder();
    private String localSha1Hex = "";
    private List<String> availableTracks = new ArrayList<>();
    private File activePackFile = null;
    private boolean initialized = false;

    public static ClientAudioPackManager getInstance() {
        return INSTANCE;
    }

    public synchronized void init() {
        if (initialized) return;
        initialized = true;

        File zipFile = locateClientPackZip();
        if (zipFile != null && zipFile.exists()) {
            this.activePackFile = zipFile;
            this.localSha1Hex = EventMusicPackBuilder.computeFileSha1Hex(zipFile);
            FracturedUtils.LOGGER.info("[ClientAudioPackManager] Successfully initialized client event_music_pack.zip from {} (SHA1: {})", zipFile.getAbsolutePath(), localSha1Hex);
        } else {
            FracturedUtils.LOGGER.info("[ClientAudioPackManager] No local event_music_pack.zip found in client directory.");
        }
    }

    public File locateClientPackZip() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path p1 = gameDir.resolve("event_music").resolve("event_music_pack.zip");
        if (p1.toFile().exists()) return p1.toFile();

        Path p2 = gameDir.resolve("fractured_utils_cache").resolve("event_music_pack.zip");
        if (p2.toFile().exists()) return p2.toFile();

        Path p3 = gameDir.resolve("resourcepacks").resolve("event_music_pack.zip");
        if (p3.toFile().exists()) return p3.toFile();

        Path tracksDir = gameDir.resolve("event_music").resolve("tracks");
        if (Files.exists(tracksDir)) {
            boolean built = packBuilder.buildPack("fracturedutils");
            if (built && packBuilder.getZipFile().toFile().exists()) {
                return packBuilder.getZipFile().toFile();
            }
        }

        return null;
    }

    public byte[] getTrackBytes(String soundEventId) {
        if (soundEventId == null) return null;
        String cleanId = soundEventId.contains(":") ? soundEventId.substring(soundEventId.indexOf(':') + 1) : soundEventId;
        if (cleanId.startsWith("event.")) cleanId = cleanId.substring(6);

        File packFile = getActivePackFile();
        if (packFile == null || !packFile.exists()) packFile = locateClientPackZip();

        if (packFile != null && packFile.exists()) {
            try (ZipFile zf = new ZipFile(packFile)) {
                String[] candidates = new String[]{
                        "assets/fracturedutils/sounds/music/" + cleanId + ".ogg",
                        "assets/fracturedutils/sounds/" + cleanId + ".ogg",
                        "assets/fractured_utils/sounds/music/" + cleanId + ".ogg",
                        "assets/minecraft/sounds/music/" + cleanId + ".ogg",
                        cleanId + ".ogg"
                };
                for (String cand : candidates) {
                    ZipEntry entry = zf.getEntry(cand);
                    if (entry != null) {
                        try (InputStream is = zf.getInputStream(entry)) {
                            return is.readAllBytes();
                        }
                    }
                }
            } catch (Exception e) {
                FracturedUtils.LOGGER.error("[ClientAudioPackManager] Error reading track bytes for " + soundEventId, e);
            }
        }

        // Search raw track paths in client directory
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path[] rawCandidates = new Path[]{
                gameDir.resolve("event_music").resolve("tracks").resolve(cleanId + ".ogg"),
                gameDir.resolve("event_music").resolve(cleanId + ".ogg"),
                gameDir.resolve("tracks").resolve(cleanId + ".ogg"),
                gameDir.resolve(cleanId + ".ogg")
        };

        for (Path p : rawCandidates) {
            if (Files.exists(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    public synchronized void handlePackSync(String serverSha1Hex, boolean required, List<String> tracks) {
        if (tracks != null && !tracks.isEmpty()) {
            this.availableTracks = new ArrayList<>(tracks);
        }

        File found = activePackFile != null && activePackFile.exists() ? activePackFile : locateClientPackZip();
        if (found != null && found.exists()) {
            this.activePackFile = found;
            this.localSha1Hex = EventMusicPackBuilder.computeFileSha1Hex(found);
        }

        ModMessages.sendToServer(new C2SResourcePackStatusPacket(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        FracturedUtils.LOGGER.info("[ClientAudioPackManager] Client event audio pack ready. Replied SUCCESSFULLY_LOADED to server.");
    }

    public File getActivePackFile() {
        return activePackFile;
    }

    public String getLocalSha1Hex() {
        return localSha1Hex;
    }

    public List<String> getAvailableTracks() {
        return availableTracks;
    }
}
