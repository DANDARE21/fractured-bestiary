package net.dandare21.fracturedutils.sound.util;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@OnlyIn(Dist.CLIENT)
public class SoundStreamPackResources implements PackResources {

    private static final AtomicLong PENDING_OFFSET_MS = new AtomicLong(0L);

    public static void setPendingOffsetMs(long offsetMs) {
        PENDING_OFFSET_MS.set(offsetMs);
    }

    private final String packId;
    private final File zipFile;
    private final String namespace;

    public SoundStreamPackResources(String packId, File zipFile, String namespace) {
        this.packId = packId;
        this.zipFile = zipFile;
        this.namespace = namespace != null ? namespace : "fracturedutils";
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        if (zipFile == null || !zipFile.exists()) return null;

        String ns = location.getNamespace();
        String path = location.getPath();

        List<String> candidatePaths = new ArrayList<>();
        candidatePaths.add("assets/" + ns + "/" + path);
        candidatePaths.add("assets/fracturedutils/" + path);

        if (!path.startsWith("sounds/")) {
            candidatePaths.add("assets/" + ns + "/sounds/" + path);
            candidatePaths.add("assets/fracturedutils/sounds/" + path);
        } else {
            String strippedPath = path.substring(7);
            candidatePaths.add("assets/" + ns + "/" + strippedPath);
            candidatePaths.add("assets/fracturedutils/" + strippedPath);
        }

        try (ZipFile zf = new ZipFile(zipFile)) {
            String matchingEntryPath = null;
            for (String candidate : candidatePaths) {
                if (zf.getEntry(candidate) != null) {
                    matchingEntryPath = candidate;
                    break;
                }
            }

            if (matchingEntryPath == null) {
                return null;
            }

            final String targetPath = matchingEntryPath;
            final long offsetMs = PENDING_OFFSET_MS.getAndSet(0L);

            return () -> {
                ZipFile openZf = new ZipFile(zipFile);
                ZipEntry entry = openZf.getEntry(targetPath);
                if (entry == null) {
                    openZf.close();
                    throw new IOException("Missing zip entry: " + targetPath);
                }

                InputStream rawStream = openZf.getInputStream(entry);
                InputStream streamToUse = (offsetMs > 500L)
                        ? new OggOffsetInputStream(rawStream, entry.getSize(), offsetMs)
                        : rawStream;

                return new FilterInputStream(streamToUse) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            openZf.close();
                        }
                    }
                };
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return Set.of(namespace, "fracturedutils", "fractured_utils", "minecraft");
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return null;
    }

    @Override
    public String packId() {
        return packId;
    }

    @Override
    public void close() {
    }
}
