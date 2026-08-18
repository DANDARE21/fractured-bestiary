package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.event.ClientAudioPackManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CAudioPackSyncPacket {

    private final String sha1Hex;
    private final boolean required;
    private final List<String> tracks;

    public S2CAudioPackSyncPacket(String sha1Hex, boolean required, List<String> tracks) {
        this.sha1Hex = sha1Hex != null ? sha1Hex : "";
        this.required = required;
        this.tracks = tracks != null ? tracks : new ArrayList<>();
    }

    public S2CAudioPackSyncPacket(FriendlyByteBuf buf) {
        this.sha1Hex = buf.readUtf();
        this.required = buf.readBoolean();
        int size = buf.readVarInt();
        this.tracks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.tracks.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.sha1Hex);
        buf.writeBoolean(this.required);
        buf.writeVarInt(this.tracks.size());
        for (String track : this.tracks) {
            buf.writeUtf(track);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientAudioPackManager.getInstance().handlePackSync(sha1Hex, required, tracks);
            });
        });
        return true;
    }

    public String getSha1Hex() { return sha1Hex; }
    public boolean isRequired() { return required; }
    public List<String> getTracks() { return tracks; }
}
