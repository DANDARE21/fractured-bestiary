package net.dandare21.fracturedutils.network.packet;

import net.dandare21.fracturedutils.sound.sequence.MusicSequenceManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSaveMusicSequencePacket {
    private final String fileName;
    private final String jsonContent;

    public C2SSaveMusicSequencePacket(String fileName, String jsonContent) {
        this.fileName = fileName != null ? fileName : "";
        this.jsonContent = jsonContent != null ? jsonContent : "";
    }

    public C2SSaveMusicSequencePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf(32767);
        this.jsonContent = buf.readUtf(262144);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileName, 32767);
        buf.writeUtf(jsonContent, 262144);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                if (player.hasPermissions(2)) {
                    boolean saved = MusicSequenceManager.getInstance().saveSequenceFile(fileName, jsonContent);
                    if (saved) {
                        player.sendSystemMessage(Component.literal("✓ Saved music sequence file '" + fileName + "' to server successfully.")
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        player.sendSystemMessage(Component.literal("❌ Failed to save music sequence file '" + fileName + "'. Invalid JSON syntax or error.")
                                .withStyle(ChatFormatting.RED));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("❌ Permission Denied: Operator level 2+ required to save music sequences to server.")
                            .withStyle(ChatFormatting.RED));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
