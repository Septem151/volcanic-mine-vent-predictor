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
    private int prevStability, stability, numChamberUpdates;

    private Client client;

    public Mine(Client client) {
        this.client = client;
        aChamber = Chamber.A(client);
        bChamber = Chamber.B(client);
        cChamber = Chamber.C(client);
        stability = prevStability = 0;
        numChamberUpdates = 0;
    }

    public int calculateStabilityDiff(int aStatus) {
        return (int) Math.floor(25 - (Math.abs(aStatus - 50) + Math.abs(bChamber.getStatus() - 50)
                + Math.abs(cChamber.getStatus() - 50)) / 3.0);
    }

    public void updateChambers() {
        aChamber.update();
        bChamber.update();
        cChamber.update();
        if (bChamber.isStatusKnown() && cChamber.isStatusKnown()) {
            numChamberUpdates++;
        }
    }

    public void updateStability() {
        prevStability = stability;
        stability = client.getVarbitValue(STABILITY_VARBIT);
    }

    public void update(boolean isChambersUpdate, boolean isStabilityUpdate) {
        if (isChambersUpdate) {
            updateChambers();
        }
        if (isStabilityUpdate) {
            updateStability();
        }
    }

    public int getStabilityDiff() {
        return stability - prevStability;
    }
}
