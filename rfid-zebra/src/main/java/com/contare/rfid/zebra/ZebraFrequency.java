package com.contare.rfid.zebra;

import com.contare.rfid.objects.RfidDeviceFrequency;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ZebraFrequency {

    BRAZIL(RfidDeviceFrequency.BRAZIL, 0x00);

    private static final Map<RfidDeviceFrequency, ZebraFrequency> _frequencies = new HashMap<>();

    static {
        for (ZebraFrequency row : values()) {
            _frequencies.put(row.frequency, row);
        }
    }

    private final RfidDeviceFrequency frequency;
    private final int index;

    ZebraFrequency(final RfidDeviceFrequency frequency, final int index) {
        this.frequency = frequency;
        this.index = index;
    }

    public static ZebraFrequency of(final RfidDeviceFrequency frequency) {
        return _frequencies.get(frequency);
    }

}
