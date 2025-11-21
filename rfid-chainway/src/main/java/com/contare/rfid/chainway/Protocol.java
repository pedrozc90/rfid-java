package com.contare.rfid.chainway;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum Protocol {

    ISO18000_6C("ISO18000-6C", 0x00),
    GB_T_29768("GB/T 29768", 0x01),
    GJB_7377_1("GJB 7377.1", 0x02),
    ISO18000_6B("ISO18000-6B", 0x03);

    private static final Map<Integer, Protocol> _masks = new HashMap<>();

    static {
        for (Protocol row : values()) {
            _masks.put(row.mask, row);
        }
    }

    private final String label;
    private final int mask;

    Protocol(final String label, final int mask) {
        this.label = label;
        this.mask = mask;
    }

    public static Protocol get(final int mask) {
        return _masks.get(mask);
    }

}
