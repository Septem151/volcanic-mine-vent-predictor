package com.volcanicmine;

import lombok.Getter;

public class ChamberUpdate {
    @Getter
    private boolean blocked, failing;

    public ChamberUpdate(boolean blocked, boolean failing) {
        this.blocked = blocked;
        this.failing = failing;
    }
}
