package com.contare.rfid.chainway;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum RFLink {

    DSB_ASK_40MHZ("DSB_ASK/FM0/40KH", 0),
    PR_ASK_250MHZ("PR_ASK/Miller4/250KHz", 1),
    PR_ASK_300MHZ("PR_ASK/Miller4/300KHz", 2),
    DSB_ASK_400MHZ("DSB_ASK/FM0/400KHz", 3);

    private static final Map<Integer, RFLink> _masks = new HashMap<>();

    static {
        for (RFLink row : values()) {
            _masks.put(row.mask, row);
        }
    }

    private final String label;
    private final int mask;

    RFLink(final String label, final int mask) {
        this.label = label;
        this.mask = mask;
    }

    public static RFLink get(final int mask) {
        return _masks.get(mask);
    }

}
