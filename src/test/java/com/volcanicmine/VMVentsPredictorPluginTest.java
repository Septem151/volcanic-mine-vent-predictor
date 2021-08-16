package com.volcanicmine;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class VMVentsPredictorPluginTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(VMVentsPredictorPlugin.class);
        RuneLite.main(args);
    }
}
