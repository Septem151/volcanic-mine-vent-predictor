package com.volcanicmine;

import lombok.Getter;
import net.runelite.api.Client;

public class Chamber {
    protected static final int VENT_UPDATE_TICKS = 10;
    protected static final int CHAMBER_STATUS_VARBIT = 5936;
    protected static final int VENT_A_VARBIT = 5939;
    protected static final int VENT_B_VARBIT = 5940;
    protected static final int VENT_C_VARBIT = 5942;

    private static final int UNKNOWN_STATUS = 127;

    private static enum Name {
        A, B, C
    }

    @Getter
    private int prevStatus, status;

    @Getter
    private boolean prevBlocked, blocked;

    @Getter
    private Name name;

    private Client client;

    private Chamber(Name name, Client client) {
        this.name = name;
        this.client = client;
    }

    public static Chamber A(Client client) {
        return new Chamber(Name.A, client);
    }

    public static Chamber B(Client client) {
        return new Chamber(Name.B, client);
    }

    public static Chamber C(Client client) {
        return new Chamber(Name.C, client);
    }

    public void update(boolean isVentUpdateTick) {
        if (isVentUpdateTick) {
            prevStatus = status;
            prevBlocked = blocked;
        }
        int chambersVarbit = client.getVarbitValue(CHAMBER_STATUS_VARBIT);
        int varbitId;
        switch (name) {
            case A:
                varbitId = VENT_A_VARBIT;
                blocked = (chambersVarbit & 1) == 1;
                break;
            case B:
                varbitId = VENT_B_VARBIT;
                blocked = ((chambersVarbit >> 1) & 1) == 1;
                break;
            default:
                varbitId = VENT_C_VARBIT;
                blocked = ((chambersVarbit >> 2) & 1) == 1;
        }
        status = client.getVarbitValue(varbitId);
    }

    public void update() {
        update(false);
    }

    public boolean isStatusKnown() {
        return status != UNKNOWN_STATUS;
    }

    public boolean isFailing() {
        return status < 40 || status > 60;
    }
}
