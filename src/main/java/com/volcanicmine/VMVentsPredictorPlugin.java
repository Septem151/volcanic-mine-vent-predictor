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
    protected static final String EARTHQUAKE_MESSAGE = "A sudden earthquake strikes the cavern!";
    protected static final Color STATUS_COLOR = new Color(102, 102, 153);
    protected static final int VM_WIDGET_GROUP_ID = 611;
    protected static final int VM_WIDGET_VENT_A_ID = 17;
    protected static final int VM_SCRIPT_ID = 2022;

    private boolean isMineStarted;
    private int curTick;
    private Mine mine;
    private Predictor predictor;
    private boolean isChamberUpdateSkipped;
    private boolean isStabilityUpdateSkipped;

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
            start();
        }

        if (chatMessage.getMessage().equals(EARTHQUAKE_MESSAGE)) {
            if (isChamberUpdateTick()) {
                isChamberUpdateSkipped = true;
            } else if (isStabilityUpdateTick()) {
                isStabilityUpdateSkipped = true;
            }
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

        if (curTick == Mine.VENT_RESET_TICK) {
            // At the 5 min reset, treat this as if it's just a brand new game
            // Return because the vent status is new, and there's a possibility
            // that a vent gets checked immediately on the same tick,
            // which could cause some issues with the prediction.
            start();
            return;
        }

        boolean shouldUpdateChambers = isChamberUpdateTick() && !isChamberUpdateSkipped;
        boolean shouldUpdateStability = isStabilityUpdateTick() && !isStabilityUpdateSkipped;

        mine.update(shouldUpdateChambers, shouldUpdateStability);

        // Manually check status of A to see if we should reset
        // The reason we need to manually check is because if we don't,
        // we'll only stop updating the text of the Vent Status widget
        // on the next chamber or stability update.
        int aStatus = client.getVarbitValue(Chamber.VENT_A_VARBIT);
        if (aStatus != Chamber.UNKNOWN_STATUS) {
            return;
        }

        isChamberUpdateSkipped = false;
        isStabilityUpdateSkipped = false;

        log.info("Num chamber updates: " + mine.getNumChamberUpdates());

        if (mine.getBChamber().isStatusKnown() && mine.getCChamber().isStatusKnown()) {
            // Make predictions for A but wait for a little bit first
            // to gather some initial data
            if (mine.getNumChamberUpdates() > 2) {
                predictor.update(shouldUpdateChambers, shouldUpdateStability);
                setVentStatusText();
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

    private boolean isChamberUpdateTick() {
        return curTick % Chamber.VENT_UPDATE_TICKS == 0;
    }

    private boolean isStabilityUpdateTick() {
        return curTick % Mine.STABILITY_UPDATE_TICKS == 0;
    }

    private void start() {
        isMineStarted = true;
        mine = new Mine(client);
        predictor = new APredictor(mine);
    }

    private void reset() {
        isMineStarted = false;
        curTick = -1;
        mine = null;
        predictor = null;
    }

    private void setVentStatusText() {
        Widget ventWidget = client.getWidget(VM_WIDGET_GROUP_ID, VM_WIDGET_VENT_A_ID);
        ventWidget.setText(
                "A: " + ColorUtil.wrapWithColorTag(predictor.getInGameString(), STATUS_COLOR));
    }
}
