package com.volcanicmine;

import lombok.Getter;
import net.runelite.api.Client;

public class Mine {
    protected static final int STABILITY_UPDATE_TICKS = 25;
    protected static final int STABILITY_VARBIT = 5938;
    protected static final int VENT_RESET_TICK = 500;
    protected static final int FINAL_TICK = 950;

    @Getter
    private Chamber aChamber, bChamber, cChamber;

    @Getter
    private int prevStability, stability;

    private Client client;

    public Mine(Client client) {
        this.client = client;
        aChamber = Chamber.A(client);
        bChamber = Chamber.B(client);
        cChamber = Chamber.C(client);
        stability = prevStability = 0;
    }

    public void update(boolean isStabilityUpdateTick, boolean isVentUpdateTick) {
        if (isStabilityUpdateTick) {
            prevStability = stability;
        }

        stability = client.getVarbitValue(STABILITY_VARBIT);

        aChamber.update(isVentUpdateTick);
        bChamber.update(isVentUpdateTick);
        cChamber.update(isVentUpdateTick);
    }

    public boolean isEachChamberStatusKnown() {
        return aChamber.isStatusKnown() && bChamber.isStatusKnown() && cChamber.isStatusKnown();
    }
}
