package net.dandare21.fracturedutils.client;

import net.dandare21.fracturedutils.ping.HudPing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientPingData {
    private static final List<HudPing> activePings = Collections.synchronizedList(new ArrayList<>());

    public static void setPings(List<HudPing> pings) {
        activePings.clear();
        if (pings != null) {
            activePings.addAll(pings);
        }
    }

    public static List<HudPing> getActivePings() {
        synchronized (activePings) {
            return new ArrayList<>(activePings);
        }
    }
}
