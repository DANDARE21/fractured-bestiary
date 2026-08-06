package net.dandare21.fracturedutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.dandare21.fracturedutils.ping.HudPing;
import net.dandare21.fracturedutils.ping.PingManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> pingCommand = Commands.literal("ping")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> addPingAuto(ctx, StringArgumentType.getString(ctx, "name"), null, null))
                                .then(Commands.argument("label", StringArgumentType.string())
                                        .executes(ctx -> addPingAuto(ctx, StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "label"), null))
                                )
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> addPingExplicit(ctx, StringArgumentType.getString(ctx, "name"), Vec3Argument.getCoordinates(ctx, "pos").getPosition(ctx.getSource()), null, null))
                                        .then(Commands.argument("label", StringArgumentType.string())
                                                .executes(ctx -> addPingExplicit(ctx, StringArgumentType.getString(ctx, "name"), Vec3Argument.getCoordinates(ctx, "pos").getPosition(ctx.getSource()), StringArgumentType.getString(ctx, "label"), null))
                                                .then(Commands.argument("colorHex", StringArgumentType.word())
                                                        .executes(ctx -> addPingExplicit(ctx, StringArgumentType.getString(ctx, "name"), Vec3Argument.getCoordinates(ctx, "pos").getPosition(ctx.getSource()), StringArgumentType.getString(ctx, "label"), StringArgumentType.getString(ctx, "colorHex")))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(PingCommand::removePing)))
                .then(Commands.literal("clear")
                        .executes(PingCommand::clearPings))
                .then(Commands.literal("list")
                        .executes(PingCommand::listPings));

        dispatcher.register(pingCommand);

        // Register aliases to avoid collisions with other mods
        dispatcher.register(Commands.literal("gmarker").redirect(dispatcher.getRoot().getChild("ping")));
        dispatcher.register(Commands.literal("hudmarker").redirect(dispatcher.getRoot().getChild("ping")));
        dispatcher.register(Commands.literal("fu_ping").redirect(dispatcher.getRoot().getChild("ping")));
    }

    private static int addPingAuto(CommandContext<CommandSourceStack> ctx, String name, String label, String colorHex) {
        CommandSourceStack src = ctx.getSource();
        Vec3 pos;
        if (src.getEntity() instanceof ServerPlayer player) {
            HitResult hit = player.pick(120.0D, 0.0F, false);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                pos = hit.getLocation();
            } else {
                pos = player.position();
            }
        } else {
            pos = src.getPosition();
        }
        return addPingExplicit(ctx, name, pos, label, colorHex);
    }

    private static int addPingExplicit(CommandContext<CommandSourceStack> ctx, String name, Vec3 pos, String label, String colorHex) {
        CommandSourceStack src = ctx.getSource();
        int color = parseColor(colorHex, 0xFF00E5FF);
        String dimension = src.getLevel().dimension().location().toString();
        String creator = src.getTextName();

        HudPing ping = new HudPing(name, label != null ? label : name, pos.x, pos.y, pos.z, dimension, color, "default", creator);
        PingManager.getInstance().addPing(src.getServer(), ping);

        src.sendSuccess(() -> Component.literal(String.format("§a[Global Marker] Added HUD ping '§b%s§a' at (%.1f, %.1f, %.1f) in %s",
                name, pos.x, pos.y, pos.z, dimension)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removePing(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        boolean removed = PingManager.getInstance().removePing(src.getServer(), name);

        if (removed) {
            src.sendSuccess(() -> Component.literal("§a[Global Marker] Removed HUD ping '" + name + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            src.sendFailure(Component.literal("§c[Global Marker] Ping '" + name + "' not found."));
            return 0;
        }
    }

    private static int clearPings(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        PingManager.getInstance().clearPings(src.getServer());
        src.sendSuccess(() -> Component.literal("§a[Global Marker] Cleared all HUD pings.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listPings(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        List<HudPing> pings = PingManager.getInstance().getPings();
        if (pings.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§e[Global Marker] No active HUD pings.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        src.sendSuccess(() -> Component.literal("§b--- Active HUD Pings (" + pings.size() + ") ---").withStyle(ChatFormatting.AQUA), false);
        for (HudPing p : pings) {
            src.sendSuccess(() -> Component.literal(String.format(" §7- §b%s§7 ('%s') at (%.1f, %.1f, %.1f) [%s]",
                    p.getId(), p.getLabel(), p.getX(), p.getY(), p.getZ(), p.getDimension())), false);
        }
        return pings.size();
    }

    private static int parseColor(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() <= 6) {
                parsed |= 0xFF000000L; // Ensure full alpha if only RGB specified
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return defaultColor;
        }
    }
}
