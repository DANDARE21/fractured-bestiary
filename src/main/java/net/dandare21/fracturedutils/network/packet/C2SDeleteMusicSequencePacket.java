package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SDeleteMusicSequencePacket {
    private final String fileName;

    public C2SDeleteMusicSequencePacket(String fileName) {
        this.fileName = fileName != null ? fileName : "";
    }

    public C2SDeleteMusicSequencePacket(FriendlyByteBuf buf) {
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
                    boolean deleted = MusicSequenceManager.getInstance().deleteSequenceFile(fileName);
                    if (deleted) {
                        player.sendSystemMessage(Component.literal("✓ Deleted music sequence file '" + fileName + "' from server.")
                                .withStyle(ChatFormatting.YELLOW));
                    } else {
                        player.sendSystemMessage(Component.literal("❌ Failed to delete music sequence file '" + fileName + "'. File not found.")
                                .withStyle(ChatFormatting.RED));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("❌ Permission Denied: Operator level 2+ required to delete music sequences.")
                            .withStyle(ChatFormatting.RED));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
