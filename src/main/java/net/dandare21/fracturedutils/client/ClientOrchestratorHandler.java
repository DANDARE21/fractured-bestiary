package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.client.gui.OrchestratorScreen;
import net.minecraft.client.Minecraft;

import java.util.Map;

public class ClientOrchestratorHandler {
    public static void openScreen(Map<String, String> sequenceFiles) {
        Minecraft.getInstance().setScreen(new OrchestratorScreen(sequenceFiles));
    }
}
