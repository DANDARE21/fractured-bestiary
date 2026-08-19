package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.dandare21.fracturedutils.sound.event.EventAudioManager;
import net.dandare21.fracturedutils.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class C2SRequestOpenMusicSequenceUiPacket {

    public C2SRequestOpenMusicSequenceUiPacket() {
    }

    public C2SRequestOpenMusicSequenceUiPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.hasPermissions(2)) {
                Map<String, String> files = MusicSequenceManager.getInstance().getAllSequenceFiles();
                List<String> trackSuggestions = EventAudioManager.getInstance().getAvailableTrackSuggestions();
                ModMessages.sendToPlayer(new S2CSendMusicSequenceDataPacket(files, trackSuggestions), player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
