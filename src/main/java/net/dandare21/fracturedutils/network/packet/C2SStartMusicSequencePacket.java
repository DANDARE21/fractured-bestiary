package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.function.Supplier;

public class C2SStartMusicSequencePacket {
    private final String fileName;

    public C2SStartMusicSequencePacket(String fileName) {
        this.fileName = fileName != null ? fileName : "";
    }

    public C2SStartMusicSequencePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(32767);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileName, 32767);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (player.hasPermissions(2)) {
                    boolean started = MusicSequenceManager.getInstance().startSequence(fileName, Collections.singletonList(player));
                    if (started) {
                        player.sendSystemMessage(Component.literal("▶ Started music sequence '" + fileName + "'.")
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage(Component.literal("❌ Failed to start music sequence '" + fileName + "'. File not found or invalid JSON.")
                                .withStyle(ChatFormatting.RED));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("❌ Permission Denied: Operator level 2+ required to start music sequences.")
                            .withStyle(ChatFormatting.RED));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
