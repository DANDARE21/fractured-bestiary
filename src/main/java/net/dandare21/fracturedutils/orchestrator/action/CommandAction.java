package net.dandare21.fracturedutils.orchestrator.action;

import com.mojang.brigadier.CommandDispatcher;
import net.dandare21.fracturedutils.orchestrator.SequenceInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CommandAction implements OrchestratorAction {
    private String type = "command";
    private String run;

    public CommandAction() {
        this.run = "";
    }

    public CommandAction(String run) {
        this.run = run != null ? run : "";
    }

    public String getRun() {
        return run;
    }

    public void setRun(String run) {
        this.run = run != null ? run : "";
    }

    public static boolean isValidCommand(Minecraft minecraft, String command) {
        if (command == null || command.isBlank()) return true;
        String cmdToParse = command.trim();
        if (cmdToParse.startsWith("/")) {
            cmdToParse = cmdToParse.substring(1);
        }
        int spaceIdx = cmdToParse.indexOf(' ');
        String rootName = spaceIdx > 0 ? cmdToParse.substring(0, spaceIdx) : cmdToParse;

        try {
            if (minecraft != null && minecraft.player != null && minecraft.getConnection() != null) {
                CommandDispatcher<SharedSuggestionProvider> dispatcher = minecraft.getConnection().getCommands();
                if (dispatcher != null && dispatcher.getRoot() != null) {
                    return dispatcher.getRoot().getChild(rootName) != null;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    @Override
    public ActionResult execute(SequenceInstance instance, MinecraftServer server) {
        if (server == null || run == null || run.isBlank()) {
            return ActionResult.SUCCESS;
        }

        String targetPlayerName = instance.getTargetPlayerName();
        String command = run.replace("%player%", targetPlayerName != null ? targetPlayerName : "");
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        CommandSourceStack source;
        ServerPlayer targetPlayer = (targetPlayerName != null && !targetPlayerName.isBlank())
                ? server.getPlayerList().getPlayerByName(targetPlayerName)
                : null;

        if (targetPlayer != null) {
            source = targetPlayer.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();
        } else {
            source = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();
        }

        server.getCommands().performPrefixedCommand(source, command);
        return ActionResult.SUCCESS;
    }

    @Override
    public String getType() {
        return "command";
    }

    @Override
    public OrchestratorAction copy() {
        return new CommandAction(this.run);
    }
}
