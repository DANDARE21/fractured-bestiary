package net.dandare21.fracturedutils.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.dandare21.fracturedutils.client.gui.CinematicScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.dandare21.fracturedutils.FracturedUtils;
import net.dandare21.fracturedutils.network.ModMessages;
import net.dandare21.fracturedutils.network.packet.C2SClientReadyPacket;
import net.dandare21.fracturedutils.network.packet.C2SCutsceneEndPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClientCutsceneHandler {

    private static final ClientCutsceneHandler INSTANCE = new ClientCutsceneHandler();

    private UUID activeCutsceneId;
    private long scheduledStartTimeMs = -1L;
    private boolean isPlaybackStarted = false;
    private boolean isCutsceneActive = false;

    private final Map<SoundSource, Float> originalSoundSourceVolumes = new EnumMap<>(SoundSource.class);
    private boolean isAudioDucked = false;
    private boolean isUserInEscMenu = false;
    public int getUserVolumePercent() {
        return ClientCutsceneConfig.getVideoVolumePercent();
    }

    public void setUserVolumePercent(int percent) {
        ClientCutsceneConfig.setVideoVolumePercent(percent);
    }

    public void adjustUserVolume(int delta) {
        setUserVolumePercent(getUserVolumePercent() + delta);
    }

    // WATERMeDIA player reference
    private Object mediaPlayerInstance;
    private String currentLocalUri;
    private boolean isWaterMediaAvailable = false;
    private String errorMessage = null;
    private int errorDisplayTimer = 0;

    public static ClientCutsceneHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Checks if WATERMeDIA mod is loaded in the Forge ModList or present on the classpath.
     */
    public boolean isWaterMediaInstalled() {
        boolean modListHasIt = ModList.get().isLoaded("watermedia") || ModList.get().isLoaded("watermedia_api") || ModList.get().isLoaded("watermedia-api");
        FracturedUtils.LOGGER.info("[Cutscene] ModList check for watermedia: " + modListHasIt);

        if (modListHasIt) return true;

        String[] probeClasses = new String[] {
                "org.watermedia.api.player.videolan.BasePlayer",
                "org.watermedia.api.player.videolan.VideoPlayer",
                "org.watermedia.api.player.SyncVideoPlayer",
                "org.watermedia.api.player.SyncBasePlayer",
                "org.watermedia.api.player.MediaPlayer",
                "org.watermedia.api.player.MediaAPI",
                "org.watermedia.api.player.PlayerAPI",
                "me.srrapero720.watermedia.api.player.SyncVideoPlayer",
                "me.srrapero720.watermedia.api.player.videolan.VideoPlayer"
        };
        for (String cls : probeClasses) {
            try {
                Class.forName(cls);
                FracturedUtils.LOGGER.info("[Cutscene] Classpath probe found WATERMeDIA class: " + cls);
                return true;
            } catch (ClassNotFoundException ignored) {}
        }

        return false;
    }

    /**
     * Scans the installed WATERMeDIA jar file for potential media player and MRL classes dynamically.
     */
    private List<Class<?>> discoverWaterMediaClasses() {
        Set<Class<?>> classes = new LinkedHashSet<>();
        ClassLoader loader = Minecraft.getInstance().getClass().getClassLoader();

        String[] staticCandidates = new String[] {
                "org.watermedia.api.player.videolan.VideoPlayer",
                "org.watermedia.api.player.videolan.BasePlayer",
                "org.watermedia.api.player.SyncVideoPlayer",
                "org.watermedia.api.player.SyncBasePlayer",
                "org.watermedia.api.media.players.FFMediaPlayer",
                "org.watermedia.api.media.players.TxMediaPlayer",
                "org.watermedia.api.media.players.MediaPlayer",
                "org.watermedia.api.player.MediaPlayer",
                "me.srrapero720.watermedia.api.player.videolan.VideoPlayer",
                "me.srrapero720.watermedia.api.player.videolan.BasePlayer",
                "me.srrapero720.watermedia.api.player.SyncVideoPlayer",
                "me.srrapero720.watermedia.api.player.SyncBasePlayer",
                "me.srrapero720.watermedia.api.player.MediaPlayer",
                "org.watermedia.videolan.VideoPlayer",
                "org.watermedia.videolan.BasePlayer",
                "org.watermedia.player.VideoPlayer",
                "org.watermedia.api.media.MRL",
                "org.watermedia.api.image.MRL",
                "me.srrapero720.watermedia.api.image.MRL"
        };

        for (String name : staticCandidates) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                classes.add(c);
            } catch (Throwable ignored) {}
        }

        try {
            var modContainer = ModList.get().getModContainerById("watermedia");
            if (modContainer.isEmpty()) modContainer = ModList.get().getModContainerById("watermedia_api");
            if (modContainer.isEmpty()) modContainer = ModList.get().getModContainerById("watermedia-api");

            modContainer.ifPresent(container -> {
                try {
                    Path jarPath = container.getModInfo().getOwningFile().getFile().getFilePath();
                    if (jarPath != null && jarPath.toFile().exists() && jarPath.toString().endsWith(".jar")) {
                        try (JarFile jar = new JarFile(jarPath.toFile())) {
                            Enumeration<JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                String eName = entry.getName();
                                if (eName.endsWith(".class") && !eName.contains("package-info")) {
                                    String className = eName.replace('/', '.').substring(0, eName.length() - 6);
                                    String lower = className.toLowerCase(Locale.ROOT);
                                    if ((lower.contains(".player.") || lower.contains("player")) && !lower.contains("server")) {
                                        try {
                                            Class<?> loadedClass = Class.forName(className, false, loader);
                                            classes.add(loadedClass);
                                        } catch (Throwable ignored) {}
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    FracturedUtils.LOGGER.warn("[Cutscene] Dynamic WATERMeDIA jar scanner exception: " + t.getMessage(), t);
                }
            });
        } catch (Throwable t) {
            FracturedUtils.LOGGER.warn("[Cutscene] ModContainer lookup exception: " + t.getMessage(), t);
        }

        List<Class<?>> sorted = new ArrayList<>(classes);
        sorted.sort((c1, c2) -> {
            boolean v1 = c1.getName().endsWith("VideoPlayer") || c1.getName().endsWith("BasePlayer");
            boolean v2 = c2.getName().endsWith("VideoPlayer") || c2.getName().endsWith("BasePlayer");
            if (v1 && !v2) return -1;
            if (!v1 && v2) return 1;
            return c1.getName().compareTo(c2.getName());
        });

        return sorted;
    }

    private boolean allowSkip = false;
    private boolean deleteAfter = true;
    private File downloadedFile = null;
    private String customName = null;

    public boolean isAllowSkip() {
        return allowSkip;
    }

    public boolean isDeleteAfter() {
        return deleteAfter;
    }

    public String getCustomName() {
        return customName;
    }

    /**
     * Called when S2CPrepareVideoPacket is received.
     */
    public void prepareVideo(String videoUrl, UUID cutsceneId, boolean allowSkip, boolean deleteAfter, String customName) {
        FracturedUtils.LOGGER.info("[Cutscene] prepareVideo called for URL: " + videoUrl + ", Cutscene ID: " + cutsceneId + ", allowSkip: " + allowSkip + ", deleteAfter: " + deleteAfter + ", customName: " + customName);
        this.activeCutsceneId = cutsceneId;
        this.allowSkip = allowSkip;
        this.deleteAfter = deleteAfter;
        this.customName = customName;
        this.downloadedFile = null;
        this.scheduledStartTimeMs = -1L;
        this.isPlaybackStarted = false;
        this.isCutsceneActive = true;
        this.errorMessage = null;
        this.errorDisplayTimer = 0;

        if (!isWaterMediaInstalled()) {
            this.errorMessage = "WATERMeDIA mod is required for cinematic playback!";
            FracturedUtils.LOGGER.error("[Cutscene] Cinematic playback failed: WATERMeDIA mod is missing on client.");
            stopAndCleanup();
            return;
        }

        duckMinecraftAudio();

        ClientVideoCache.getVideoFileAsync(videoUrl, customName).thenAcceptAsync(file -> {
            onVideoDownloaded(file, cutsceneId);
        }, Minecraft.getInstance()).exceptionally(ex -> {
            FracturedUtils.LOGGER.error("[Cutscene] Failed to prepare video for cutscene " + cutsceneId, ex);
            this.errorMessage = "Failed to download video file: " + ex.getMessage();
            stopAndCleanup();
            return null;
        });
    }

    private void onVideoDownloaded(File file, UUID cutsceneId) {
        this.downloadedFile = file;
        FracturedUtils.LOGGER.info("[Cutscene] Video downloaded to local file: " + file.getAbsolutePath() + " (exists=" + file.exists() + ", length=" + file.length() + ")");
        if (!isCutsceneActive || !cutsceneId.equals(activeCutsceneId)) {
            FracturedUtils.LOGGER.warn("[Cutscene] Cutscene is no longer active or ID mismatched. Aborting preparation.");
            return;
        }

        try {
            Path canonicalPath = file.toPath().toAbsolutePath().normalize();
            URI canonicalUri = canonicalPath.toUri();
            this.currentLocalUri = canonicalUri.toString();
            FracturedUtils.LOGGER.info("[Cutscene] Normalized video local URI: " + this.currentLocalUri);

            initWaterMediaPlayer(canonicalUri, file);

            // Hold paused on frame 0
            pausePlayer();

            // Notify server that client is 100% prepared and ready
            FracturedUtils.LOGGER.info("[Cutscene] Client preparation complete. Sending C2SClientReadyPacket for cutscene: " + cutsceneId);
            ModMessages.sendToServer(new C2SClientReadyPacket(cutsceneId));
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[Cutscene] Error initializing WATERMeDIA player for " + currentLocalUri, e);
            this.errorMessage = "Error initializing WATERMeDIA player: " + e.getMessage();
            stopAndCleanup();
        }
    }

    /**
     * Called when S2CStartPlaybackPacket is received.
     */
    public void startPlayback(UUID cutsceneId, long scheduledStartTimeMs) {
        FracturedUtils.LOGGER.info("[Cutscene] Received S2CStartPlaybackPacket for cutscene: " + cutsceneId + ", scheduledStartTimeMs: " + scheduledStartTimeMs + " (diff=" + (scheduledStartTimeMs - System.currentTimeMillis()) + "ms)");
        if (!isCutsceneActive || !cutsceneId.equals(activeCutsceneId)) {
            this.activeCutsceneId = cutsceneId;
            this.isCutsceneActive = true;
        }
        this.scheduledStartTimeMs = scheduledStartTimeMs;
        this.isPlaybackStarted = false;
    }

    public void setInEscMenu(boolean inEscMenu) {
        this.isUserInEscMenu = inEscMenu;
    }

    public boolean isInEscMenu() {
        return isUserInEscMenu;
    }

    /**
     * Checked on client tick to trigger zero-latency playback at scheduledStartTimeMs.
     */
    public void onClientTick() {
        if (!isCutsceneActive) return;

        if (scheduledStartTimeMs > 0 && !isPlaybackStarted) {
            long now = System.currentTimeMillis();
            if (now >= scheduledStartTimeMs) {
                FracturedUtils.LOGGER.info("[Cutscene] Reached scheduledStartTimeMs. Triggering playback start!");
                isPlaybackStarted = true;
                playPlayer();
            }
        }

        if (isPlaybackStarted) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                this.isUserInEscMenu = false;
                mc.setScreen(new CinematicScreen());
            } else if (!isUserInEscMenu && !(mc.screen instanceof CinematicScreen)) {
                mc.setScreen(new CinematicScreen());
            }

            // Synchronize video player volume with Minecraft Master volume & user volume adjustment
            if (isWaterMediaAvailable && mediaPlayerInstance != null) {
                try {
                    float masterVolume = mc.options.getSoundSourceVolume(SoundSource.MASTER);
                    int videoVolume = Math.max(0, Math.min(100, Math.round(masterVolume * (getUserVolumePercent() / 100.0f) * 100.0f)));
                    invokePlayerMethod("volume", new Class<?>[]{int.class}, videoVolume);
                } catch (Throwable ignored) {}
            }
        }

        if (isPlaybackStarted && (errorMessage != null || !isWaterMediaAvailable)) {
            FracturedUtils.LOGGER.error("[Cutscene] Error detected during cutscene playback (" + (errorMessage != null ? errorMessage : "WATERMeDIA unavailable") + "). Closing screen.");
            stopAndCleanup();
            return;
        }

        if (isPlaybackStarted && isWaterMediaAvailable && isPlayerFinished()) {
            FracturedUtils.LOGGER.info("[Cutscene] Video playback finished naturally. Cleaning up.");
            stopAndCleanup();
        }
    }

    /**
     * Renders video frame onto HUD overlay (RenderGuiOverlayEvent.Post).
     */
    public void renderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!isCutsceneActive || !isPlaybackStarted) return;

        // If CinematicScreen is active, video is already rendered full-screen by the screen
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CinematicScreen) return;

        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            GuiGraphics guiGraphics = event.getGuiGraphics();
            renderWaterMediaFrame(guiGraphics, width, height);
        }
    }

    private void duckMinecraftAudio() {
        if (!isAudioDucked) {
            try {
                Minecraft mc = Minecraft.getInstance();
                originalSoundSourceVolumes.clear();
                for (SoundSource source : SoundSource.values()) {
                    if (source != SoundSource.MASTER) {
                        float vol = mc.options.getSoundSourceVolume(source);
                        originalSoundSourceVolumes.put(source, vol);
                        setSoundSourceVolume(mc, source, 0.0f);
                    }
                }
                isAudioDucked = true;
                FracturedUtils.LOGGER.info("[Cutscene] Muted all non-master Minecraft sound sources.");
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[Cutscene] Failed to duck Minecraft non-master audio", e);
            }
        }
    }

    private void restoreMinecraftAudio() {
        if (isAudioDucked) {
            try {
                Minecraft mc = Minecraft.getInstance();
                for (Map.Entry<SoundSource, Float> entry : originalSoundSourceVolumes.entrySet()) {
                    setSoundSourceVolume(mc, entry.getKey(), entry.getValue());
                }
                originalSoundSourceVolumes.clear();
                isAudioDucked = false;
                FracturedUtils.LOGGER.info("[Cutscene] Restored all Minecraft non-master sound sources.");
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[Cutscene] Failed to restore Minecraft audio", e);
            }
        }
    }

    private void setSoundSourceVolume(Minecraft mc, SoundSource source, float volume) {
        try {
            mc.getSoundManager().updateSourceVolume(source, volume);
        } catch (Throwable t) {
            try {
                Method m = mc.options.getClass().getMethod("getSoundSourceOption", SoundSource.class);
                Object optionInstance = m.invoke(mc.options, source);
                if (optionInstance != null) {
                    Method setMethod = optionInstance.getClass().getMethod("set", Object.class);
                    setMethod.invoke(optionInstance, (double) volume);
                }
            } catch (Throwable ignored) {}
        }
    }

    public void stopAndCleanup() {
        FracturedUtils.LOGGER.info("[Cutscene] stopAndCleanup called.");
        UUID cutsceneId = activeCutsceneId;
        isCutsceneActive = false;
        isPlaybackStarted = false;
        isUserInEscMenu = false;
        scheduledStartTimeMs = -1L;
        activeCutsceneId = null;
        errorMessage = null;
        errorDisplayTimer = 0;

        if (cutsceneId != null) {
            try {
                ModMessages.sendToServer(new C2SCutsceneEndPacket(cutsceneId));
            } catch (Throwable t) {
                FracturedUtils.LOGGER.warn("[Cutscene] Failed to send C2SCutsceneEndPacket", t);
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CinematicScreen) {
            mc.setScreen(null);
        }

        releasePlayer();
        restoreMinecraftAudio();

        if (deleteAfter && downloadedFile != null) {
            ClientVideoCache.deleteCachedVideo(downloadedFile);
            downloadedFile = null;
        }
    }

    // --- WATERMeDIA API Integration Wrappers ---

    private void initWaterMediaPlayer(URI uriObj, File fileObj) throws Exception {
        releasePlayer();
        this.isWaterMediaAvailable = false;
        String uriStr = uriObj.toString();

        List<Class<?>> candidateClasses = discoverWaterMediaClasses();
        FracturedUtils.LOGGER.info("[Cutscene] Discovered " + candidateClasses.size() + " potential WATERMeDIA candidate class(es) for media player initialization.");

        // Construct MRL handle first
        Object mrlInstance = createMrlInstance(candidateClasses, uriObj, uriStr, fileObj);
        if (mrlInstance != null) {
            FracturedUtils.LOGGER.info("[Cutscene] Successfully constructed MRL object of type: " + mrlInstance.getClass().getName());
            try {
                Method awaitMethod = mrlInstance.getClass().getMethod("await", long.class);
                boolean awaitSuccess = (boolean) awaitMethod.invoke(mrlInstance, 10000L);
                FracturedUtils.LOGGER.info("[Cutscene] MRL.await(10000) returned: " + awaitSuccess);
            } catch (Throwable t) {
                FracturedUtils.LOGGER.warn("[Cutscene] Failed to await MRL loading: " + t.getMessage());
            }
        }

        // 1. Primary Strategy: Try MediaAPI.createPlayer(MRL, Supplier<GFXEngine>, Supplier<SFXEngine>) per WATERMeDIA 3.x structure
        Object playerObj = tryCreateWaterMedia3VideoPlayer(uriObj, mrlInstance, uriStr, fileObj);
        if (playerObj != null) {
            this.mediaPlayerInstance = playerObj;
            this.isWaterMediaAvailable = true;
            this.errorMessage = null;
            FracturedUtils.LOGGER.info("[Cutscene] SUCCESS: Instantiated player via MediaAPI strategy: " + playerObj.getClass().getName());
            return;
        }

        // 2. Fallback Strategy: Generic reflection search across candidate classes
        for (Class<?> clazz : candidateClasses) {
            String className = clazz.getName();

            if (className.contains("Server") || className.endsWith("ServerMediaPlayer") || (!className.endsWith("Player") && !className.contains(".player."))) {
                continue;
            }

            FracturedUtils.LOGGER.info("[Cutscene] Evaluating fallback player candidate class: " + className);

            Object player = createVideoPlayerInstance(clazz, mrlInstance, uriObj, uriStr, fileObj);

            if (player != null) {
                this.mediaPlayerInstance = player;

                // Call start(URI) or start(MRL) to initiate video loading
                Method startMethod = findMethodWithParam(clazz, "start", URI.class, File.class, String.class, mrlInstance != null ? mrlInstance.getClass() : null);
                if (startMethod != null) {
                    try {
                        startMethod.setAccessible(true);
                        Class<?> pType = startMethod.getParameterTypes()[0];
                        FracturedUtils.LOGGER.info("[Cutscene] Calling " + className + "#start(" + pType.getSimpleName() + ").");
                        if (pType == URI.class) {
                            startMethod.invoke(this.mediaPlayerInstance, uriObj);
                        } else if (mrlInstance != null && pType.isAssignableFrom(mrlInstance.getClass())) {
                            startMethod.invoke(this.mediaPlayerInstance, mrlInstance);
                        } else if (pType == File.class) {
                            startMethod.invoke(this.mediaPlayerInstance, fileObj);
                        } else {
                            startMethod.invoke(this.mediaPlayerInstance, uriStr);
                        }
                    } catch (Throwable t) {
                        FracturedUtils.LOGGER.warn("[Cutscene] VideoPlayer#start method call failed: " + t.getMessage(), t);
                    }
                }

                this.isWaterMediaAvailable = true;
                this.errorMessage = null;
                FracturedUtils.LOGGER.info("[Cutscene] FULLY INITIALIZED WATERMeDIA player instance of class: " + className);
                break;
            }
        }

        if (!this.isWaterMediaAvailable) {
            this.errorMessage = "WATERMeDIA player instance could not be constructed!";
            FracturedUtils.LOGGER.error("[Cutscene] WATERMeDIA mod was detected but player class could not be initialized for URI: " + uriStr);
        }
    }

    private Object tryCreateWaterMedia3VideoPlayer(URI uriObj, Object mrlInstance, String uriStr, File fileObj) {
        if (mrlInstance == null) return null;
        try {
            Class<?> mediaApiClass = Class.forName("org.watermedia.api.media.MediaAPI");
            Class<?> gfxEngineClass = Class.forName("org.watermedia.api.media.engines.GFXEngine");
            Class<?> sfxEngineClass = Class.forName("org.watermedia.api.media.engines.SFXEngine");

            Object gfxEngine = resolveEngineInstance(gfxEngineClass);
            Object sfxEngine = resolveEngineInstance(sfxEngineClass);

            java.util.function.Supplier<?> gfxSupplier = () -> gfxEngine;
            java.util.function.Supplier<?> sfxSupplier = () -> sfxEngine;

            Method createPlayerMethod = mediaApiClass.getMethod("createPlayer", mrlInstance.getClass(), java.util.function.Supplier.class, java.util.function.Supplier.class);
            Object player = createPlayerMethod.invoke(null, mrlInstance, gfxSupplier, sfxSupplier);
            if (player != null && isValidPlayerInstance(player)) {
                FracturedUtils.LOGGER.info("[Cutscene] MediaAPI.createPlayer returned valid player: " + player.getClass().getName());
                return player;
            }
        } catch (Throwable t) {
            FracturedUtils.LOGGER.warn("[Cutscene] MediaAPI.createPlayer invocation failed: " + t.getMessage(), t);
        }
        return null;
    }

    private Object resolveEngineInstance(Class<?> engineClass) {
        if (engineClass == null) return null;
        String eName = engineClass.getName();

        // 1. GFXEngine resolution (GLEngine.Builder)
        if (eName.contains("GFXEngine") || eName.contains("GfxEngine")) {
            try {
                Class<?> builderClass = Class.forName("org.watermedia.api.media.engines.GLEngine$Builder");
                Constructor<?> bCtor = builderClass.getConstructor(Thread.class, java.util.concurrent.Executor.class);
                Object builderObj = bCtor.newInstance(Thread.currentThread(), Minecraft.getInstance());
                Method buildMethod = builderClass.getMethod("build");
                Object glEngine = buildMethod.invoke(builderObj);
                if (glEngine != null) {
                    FracturedUtils.LOGGER.info("[Cutscene] Created GFXEngine via GLEngine$Builder: " + glEngine.getClass().getName());
                    return glEngine;
                }
            } catch (Throwable t) {
                FracturedUtils.LOGGER.warn("[Cutscene] Failed to build GLEngine via Builder: " + t.getMessage());
            }
        }

        // 2. SFXEngine resolution (ALEngine.buildDefault)
        if (eName.contains("SFXEngine") || eName.contains("SfxEngine")) {
            try {
                Class<?> alEngineClass = Class.forName("org.watermedia.api.media.engines.ALEngine");
                Method buildDefaultMethod = alEngineClass.getMethod("buildDefault");
                Object alEngine = buildDefaultMethod.invoke(null);
                if (alEngine != null) {
                    FracturedUtils.LOGGER.info("[Cutscene] Created SFXEngine via ALEngine.buildDefault(): " + alEngine.getClass().getName());
                    return alEngine;
                }
            } catch (Throwable t) {
                try {
                    Class<?> alEngineClass = Class.forName("org.watermedia.api.media.engines.ALEngine");
                    Constructor<?> alCtor = alEngineClass.getConstructor(int.class);
                    Object alEngine = alCtor.newInstance(0);
                    if (alEngine != null) {
                        FracturedUtils.LOGGER.info("[Cutscene] Created SFXEngine via ALEngine(0): " + alEngine.getClass().getName());
                        return alEngine;
                    }
                } catch (Throwable t2) {
                    FracturedUtils.LOGGER.warn("[Cutscene] Failed to build ALEngine: " + t2.getMessage());
                }
            }
        }

        try {
            // 3. Check static fields
            for (Field f : engineClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && engineClass.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(null);
                    if (val != null) {
                        FracturedUtils.LOGGER.info("[Cutscene] Found " + engineClass.getSimpleName() + " via field " + f.getName());
                        return val;
                    }
                }
            }
            // 4. Check static methods
            for (Method m : engineClass.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0 && engineClass.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object val = m.invoke(null);
                    if (val != null) {
                        FracturedUtils.LOGGER.info("[Cutscene] Found " + engineClass.getSimpleName() + " via method " + m.getName());
                        return val;
                    }
                }
            }
            // 5. Try dynamic proxy for interface
            if (engineClass.isInterface()) {
                Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                        engineClass.getClassLoader(),
                        new Class<?>[]{engineClass},
                        (p, method, args) -> {
                            Class<?> rt = method.getReturnType();
                            if (rt == boolean.class) return true;
                            if (rt == int.class) return 0;
                            if (rt == float.class) return 1.0f;
                            if (rt == double.class) return 1.0d;
                            return null;
                        }
                );
                FracturedUtils.LOGGER.info("[Cutscene] Created dynamic proxy fallback for " + engineClass.getSimpleName());
                return proxy;
            }
        } catch (Throwable t) {
            FracturedUtils.LOGGER.warn("[Cutscene] Exception resolving engine for " + engineClass.getName() + ": " + t.getMessage(), t);
        }
        return null;
    }

    private Object createVideoPlayerInstance(Class<?> clazz, Object mrlInstance, URI uriObj, String uriStr, File fileObj) {
        Object factoryObj = null;
        Object executorObj = Executors.newSingleThreadExecutor();

        try {
            Class<?> factoryClass = null;
            try { factoryClass = Class.forName("org.watermedia.api.player.videolan.MediaPlayerFactory"); } catch (Throwable ignored) {}
            if (factoryClass == null) { try { factoryClass = Class.forName("me.srrapero720.watermedia.api.player.videolan.MediaPlayerFactory"); } catch (Throwable ignored) {} }

            if (factoryClass != null) {
                try {
                    Constructor<?> fCtor = factoryClass.getDeclaredConstructor();
                    fCtor.setAccessible(true);
                    factoryObj = fCtor.newInstance();
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Try constructors
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            try {
                ctor.setAccessible(true);
                Class<?>[] pTypes = ctor.getParameterTypes();
                Object[] args = new Object[pTypes.length];
                boolean valid = true;

                for (int i = 0; i < pTypes.length; i++) {
                    Class<?> pt = pTypes[i];
                    if (pt.getName().contains("GFXEngine") || pt.getName().contains("GfxEngine")) {
                        args[i] = resolveEngineInstance(pt);
                    } else if (pt.getName().contains("SFXEngine") || pt.getName().contains("SfxEngine")) {
                        args[i] = resolveEngineInstance(pt);
                    } else if (factoryObj != null && pt.isAssignableFrom(factoryObj.getClass())) {
                        args[i] = factoryObj;
                    } else if (executorObj != null && pt.isAssignableFrom(executorObj.getClass())) {
                        args[i] = executorObj;
                    } else if (mrlInstance != null && pt.isAssignableFrom(mrlInstance.getClass())) {
                        args[i] = mrlInstance;
                    } else if (pt == URI.class) {
                        args[i] = uriObj;
                    } else if (pt == String.class) {
                        args[i] = uriStr;
                    } else if (pt == File.class) {
                        args[i] = fileObj;
                    } else if (pt == URL.class) {
                        try { args[i] = uriObj.toURL(); } catch (Throwable ignored) { valid = false; break; }
                    } else if (!pt.isPrimitive()) {
                        args[i] = null;
                    } else if (pt == boolean.class) {
                        args[i] = false;
                    } else if (pt == int.class || pt == Integer.class) {
                        args[i] = 0; // Default sourceIndex 0
                    } else {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    FracturedUtils.LOGGER.info("[Cutscene] Attempting " + clazz.getName() + " constructor with params: " + Arrays.toString(pTypes) + " and args: " + Arrays.toString(args));
                    Object candidate = ctor.newInstance(args);
                    if (isValidPlayerInstance(candidate)) {
                        FracturedUtils.LOGGER.info("[Cutscene] SUCCESS: Instantiated " + clazz.getName() + " via constructor.");
                        return candidate;
                    }
                }
            } catch (Throwable t) {
                FracturedUtils.LOGGER.warn("[Cutscene] Constructor with params " + Arrays.toString(ctor.getParameterTypes()) + " on " + clazz.getName() + " failed: " + t.getMessage(), t);
            }
        }

        // Try static factory methods
        for (Method m : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() != void.class) {
                try {
                    m.setAccessible(true);
                    Class<?>[] pTypes = m.getParameterTypes();
                    Object[] args = createMethodArgs(pTypes, mrlInstance, uriObj, uriStr, fileObj);
                    if (args != null) {
                        Object candidate = m.invoke(null, args);
                        if (isValidPlayerInstance(candidate)) {
                            FracturedUtils.LOGGER.info("[Cutscene] SUCCESS: Instantiated " + clazz.getName() + " via static factory method " + m.getName());
                            return candidate;
                        }
                    }
                } catch (Throwable t) {
                    FracturedUtils.LOGGER.warn("[Cutscene] Factory method " + m.getName() + " on " + clazz.getName() + " failed: " + t.getMessage(), t);
                }
            }
        }

        return null;
    }

    private Method findMethodWithParam(Class<?> clazz, String methodName, Class<?>... allowedParamTypes) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                Class<?> pt = m.getParameterTypes()[0];
                for (Class<?> allowed : allowedParamTypes) {
                    if (allowed != null && pt.isAssignableFrom(allowed)) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidPlayerInstance(Object obj) {
        if (obj == null) return false;
        Class<?> cls = obj.getClass();
        String cName = cls.getName();
        if (cName.contains("Server") || cName.contains("API") || cName.equals("org.watermedia.WaterMedia") || cName.equals("me.srrapero720.watermedia.WaterMedia")) {
            return false;
        }
        if (cls.isPrimitive() || String.class.isAssignableFrom(cls) || File.class.isAssignableFrom(cls) || Path.class.isAssignableFrom(cls) || URI.class.isAssignableFrom(cls)) {
            return false;
        }
        boolean hasRender = hasAnyMethod(cls, "render", "renderFrame", "draw", "texture");
        boolean hasControl = hasAnyMethod(cls, "start", "play", "pause", "setPause", "startPaused", "seekTo");
        return hasRender || hasControl;
    }

    private boolean hasAnyMethod(Class<?> cls, String... names) {
        for (Method m : cls.getMethods()) {
            for (String n : names) {
                if (m.getName().equals(n)) return true;
            }
        }
        return false;
    }

    private Object createMrlInstance(List<Class<?>> candidateClasses, URI uriObj, String uriStr, File fileObj) {
        for (Class<?> clazz : candidateClasses) {
            String cName = clazz.getName();
            if (cName.endsWith(".MRL") || cName.contains("MRL") || cName.endsWith("ImageAPI") || cName.endsWith("MediaAPI")) {
                FracturedUtils.LOGGER.info("[Cutscene] Probing MRL candidate class: " + cName);

                for (Method m : clazz.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() != void.class) {
                        try {
                            m.setAccessible(true);
                            Class<?>[] pTypes = m.getParameterTypes();
                            if (pTypes.length == 1) {
                                if (pTypes[0] == URI.class) {
                                    Object res = m.invoke(null, uriObj);
                                    if (res != null) {
                                        FracturedUtils.LOGGER.info("[Cutscene] Created MRL via static method " + m.getName() + "(URI) on " + cName);
                                        return res;
                                    }
                                } else if (pTypes[0] == String.class) {
                                    Object res = m.invoke(null, uriStr);
                                    if (res != null) {
                                        FracturedUtils.LOGGER.info("[Cutscene] Created MRL via static method " + m.getName() + "(String) on " + cName);
                                        return res;
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            FracturedUtils.LOGGER.debug("[Cutscene] MRL static method probe on " + cName + "." + m.getName() + " failed: " + t.getMessage());
                        }
                    }
                }

                if (cName.endsWith("MRL")) {
                    for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                        try {
                            ctor.setAccessible(true);
                            Class<?>[] pTypes = ctor.getParameterTypes();
                            if (pTypes.length == 1) {
                                if (pTypes[0] == URI.class) {
                                    Object res = ctor.newInstance(uriObj);
                                    FracturedUtils.LOGGER.info("[Cutscene] Created MRL via constructor(URI) on " + cName);
                                    return res;
                                } else if (pTypes[0] == String.class) {
                                    Object res = ctor.newInstance(uriStr);
                                    FracturedUtils.LOGGER.info("[Cutscene] Created MRL via constructor(String) on " + cName);
                                    return res;
                                }
                            }
                        } catch (Throwable t) {
                            FracturedUtils.LOGGER.debug("[Cutscene] MRL constructor probe on " + cName + " failed: " + t.getMessage());
                        }
                    }
                }
            }
        }
        return null;
    }

    private Object[] createMethodArgs(Class<?>[] pTypes, Object mrlInstance, URI uriObj, String uriStr, File fileObj) {
        Object[] args = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            Class<?> pt = pTypes[i];
            if (pt.getName().contains("GFXEngine") || pt.getName().contains("GfxEngine")) {
                args[i] = resolveEngineInstance(pt);
            } else if (pt.getName().contains("SFXEngine") || pt.getName().contains("SfxEngine")) {
                args[i] = resolveEngineInstance(pt);
            } else if (mrlInstance != null && pt.isAssignableFrom(mrlInstance.getClass())) {
                args[i] = mrlInstance;
            } else if (pt == String.class) {
                args[i] = uriStr;
            } else if (pt == URI.class) {
                args[i] = uriObj;
            } else if (pt == File.class) {
                args[i] = fileObj;
            } else if (pt == URL.class) {
                try { args[i] = uriObj.toURL(); } catch (Throwable ignored) { return null; }
            } else if (!pt.isPrimitive()) {
                args[i] = null;
            } else if (pt == boolean.class) {
                args[i] = false;
            } else if (pt == int.class || pt == Integer.class) {
                args[i] = 0; // Default sourceIndex 0
            } else {
                return null;
            }
        }
        return args;
    }

    private void pausePlayer() {
        if (mediaPlayerInstance == null) return;
        FracturedUtils.LOGGER.info("[Cutscene] Invoking pause on WATERMeDIA player instance.");
        invokePlayerMethod("startPaused", new Class<?>[]{}, (Object[]) null);
        invokePlayerMethod("setPause", new Class<?>[]{boolean.class}, true);
        invokePlayerMethod("pause", new Class<?>[]{}, (Object[]) null);
    }

    private void playPlayer() {
        if (mediaPlayerInstance == null) return;
        FracturedUtils.LOGGER.info("[Cutscene] Invoking play/start on WATERMeDIA player instance.");
        invokePlayerMethod("start", new Class<?>[]{}, (Object[]) null);
        invokePlayerMethod("resume", new Class<?>[]{}, (Object[]) null);
        invokePlayerMethod("setPause", new Class<?>[]{boolean.class}, false);
        invokePlayerMethod("play", new Class<?>[]{}, (Object[]) null);

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || !(mc.screen instanceof CinematicScreen)) {
            mc.setScreen(new CinematicScreen());
        }
    }

    private boolean isPlayerFinished() {
        if (mediaPlayerInstance == null) return false;
        Object ended = invokePlayerMethod("ended", new Class<?>[]{}, (Object[]) null);
        if (ended instanceof Boolean b) return b;

        Object isEnded = invokePlayerMethod("isEnded", new Class<?>[]{}, (Object[]) null);
        if (isEnded instanceof Boolean b) return b;

        Object playing = invokePlayerMethod("playing", new Class<?>[]{}, (Object[]) null);
        if (playing instanceof Boolean b) return !b && isPlaybackStarted;

        Object isPlaying = invokePlayerMethod("isPlaying", new Class<?>[]{}, (Object[]) null);
        if (isPlaying instanceof Boolean b) return !b && isPlaybackStarted;

        return false;
    }

    public void renderWaterMediaFrame(GuiGraphics guiGraphics, int width, int height) {
        if (mediaPlayerInstance != null && isWaterMediaAvailable && errorMessage == null) {
            try {
                // Try direct render method if available on player
                try {
                    Method renderMethod = mediaPlayerInstance.getClass().getMethod("render", GuiGraphics.class, int.class, int.class);
                    renderMethod.invoke(mediaPlayerInstance, guiGraphics, width, height);
                    return;
                } catch (NoSuchMethodException ignored1) {
                    try {
                        Method renderMethod = mediaPlayerInstance.getClass().getMethod("render", GuiGraphics.class, int.class, int.class, int.class, int.class);
                        renderMethod.invoke(mediaPlayerInstance, guiGraphics, 0, 0, width, height);
                        return;
                    } catch (NoSuchMethodException ignored2) {}
                }

                // WaterMedia 3.x OpenGL texture rendering
                Method textureMethod = mediaPlayerInstance.getClass().getMethod("texture");
                Object texObj = textureMethod.invoke(mediaPlayerInstance);
                long textureIdLong = (texObj instanceof Number n) ? n.longValue() : 0L;
                int textureId = (int) textureIdLong;

                if (textureId > 0) {
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, textureId);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder bufferBuilder = tesselator.getBuilder();
                    bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                    bufferBuilder.vertex(0, height, 0).uv(0.0F, 1.0F).endVertex();
                    bufferBuilder.vertex(width, height, 0).uv(1.0F, 1.0F).endVertex();
                    bufferBuilder.vertex(width, 0, 0).uv(1.0F, 0.0F).endVertex();
                    bufferBuilder.vertex(0, 0, 0).uv(0.0F, 0.0F).endVertex();
                    tesselator.end();
                    return;
                } else {
                    // Video is buffering / loading initial frame: remain on solid black screen
                    return;
                }
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[Cutscene] Exception while rendering WaterMedia frame: " + e.getMessage(), e);
                return;
            }
        }

        // Only close screen if an explicit error occurred or WATERMeDIA is unavailable
        if (errorMessage != null || !isWaterMediaAvailable) {
            FracturedUtils.LOGGER.error("[Cutscene] Cinematic rendering failed (" + (errorMessage != null ? errorMessage : "WATERMeDIA unavailable") + "). Closing screen.");
            stopAndCleanup();
        }
    }

    private void releasePlayer() {
        if (mediaPlayerInstance != null) {
            FracturedUtils.LOGGER.info("[Cutscene] Releasing WATERMeDIA player instance.");
            invokePlayerMethod("release", new Class<?>[]{}, (Object[]) null);
            invokePlayerMethod("stop", new Class<?>[]{}, (Object[]) null);
            mediaPlayerInstance = null;
        }
    }

    private Object invokePlayerMethod(String methodName, Class<?>[] paramTypes, Object... args) {
        if (mediaPlayerInstance == null) return null;
        try {
            Method method = mediaPlayerInstance.getClass().getMethod(methodName, paramTypes);
            return method.invoke(mediaPlayerInstance, args);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            FracturedUtils.LOGGER.debug("[Cutscene] Method " + methodName + " invocation on WATERMeDIA player failed", e);
        }
        return null;
    }
}
