package com.volcanicmine;

import java.awt.Color;
import java.util.Set;
import javax.inject.Inject;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;

@Slf4j
@PluginDescriptor(name = "Volcanic Mine Vents Predictor")
public class VMVentsPredictorPlugin extends Plugin {
    protected static final Set<Integer> MAP_REGIONS = ImmutableSet.of(15262, 15263);
    protected static final String START_MESSAGE =
            "The volcano awakens! You can now access the area below...";
    protected static final Color STATUS_COLOR = new Color(102, 102, 153);
    protected static final int VM_WIDGET_GROUP_ID = 611;
    protected static final int VM_WIDGET_VENT_A_ID = 17;

    private boolean isMineStarted;
    private int curTick;
    private Mine mine;
    private Predictor predictor;
    private boolean isAfterFirstUpdateTick;

    @Inject
    private Client client;

    @Inject
    private VMVentsPredictorConfig config;

    @Override
    protected void startUp() throws Exception {
        reset();
    }

    @Override
    protected void shutDown() throws Exception {
        reset();
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        // If the chat message is not a Game Message, return early
        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE
                && chatMessage.getType() != ChatMessageType.SPAM) {
            return;
        }

        // Start counting game ticks when the mine starts
        if (chatMessage.getMessage().equals(START_MESSAGE)) {
            isMineStarted = true;
            mine = new Mine(client);
            predictor = new Predictor(mine);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!isMineStarted) {
            return;
        }

        if (!isInVolcanicMine() || curTick == Mine.FINAL_TICK - 1) {
            reset();
            return;
        }

        curTick++;

        // if (curTick < Mine.VENT_RESET_TICK) {
        // return;
        // }

        boolean isStabilityUpdateTick = curTick % Mine.STABILITY_UPDATE_TICKS == 0;
        boolean isVentUpdateTick = curTick % Chamber.VENT_UPDATE_TICKS == 0;

        mine.update(isStabilityUpdateTick, isVentUpdateTick);

        // If A chamber has been checked, reset the plugin
        // and stop counting.
        if (mine.getAChamber().isStatusKnown()) {
            reset();
            return;
        }

        if (!mine.getAChamber().isStatusKnown() && mine.getBChamber().isStatusKnown()
                && mine.getCChamber().isStatusKnown()) {
            if (isAfterFirstUpdateTick && isVentUpdateTick) {
                predictor.predictOnVentUpdate();
            }
            if (isAfterFirstUpdateTick && isStabilityUpdateTick) {
                predictor.predictOnStabilityUpdate();
            }
            setVentStatusText();
            if (isVentUpdateTick) {
                isAfterFirstUpdateTick = true;
            }
        }
    }

    @Provides
    VMVentsPredictorConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VMVentsPredictorConfig.class);
    }

    private boolean isInVolcanicMine() {
        int playerRegion =
                WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation())
                        .getRegionID();
        return MAP_REGIONS.contains(playerRegion);
    }

    private void reset() {
        isMineStarted = false;
        curTick = -1;
        mine = null;
        predictor = null;
        isAfterFirstUpdateTick = false;
    }

    private void setVentStatusText() {
        Prediction prediction = predictor.getPrediction();
        Widget ventWidget = client.getWidget(VM_WIDGET_GROUP_ID, VM_WIDGET_VENT_A_ID);
        String text;
        if (prediction == null) {
            text = "(?)%";
        } else {
            text = prediction.toString();
        }
        ventWidget.setText("A: " + ColorUtil.wrapWithColorTag(text, STATUS_COLOR));
    }
}
