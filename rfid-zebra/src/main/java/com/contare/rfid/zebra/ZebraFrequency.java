package com.contare.rfid.zebra;

import com.contare.rfid.devices.RfidDevice;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ZebraFrequency {

    BRAZIL(RfidDevice.Frequency.BRAZIL, 0x00);

    private static final Map<RfidDevice.Frequency, ZebraFrequency> _frequencies = new HashMap<>();

    static {
        for (ZebraFrequency row : values()) {
            _frequencies.put(row.frequency, row);
        }
    }

    private final RfidDevice.Frequency frequency;
    private final int index;

    ZebraFrequency(final RfidDevice.Frequency frequency, final int index) {
        this.frequency = frequency;
        this.index = index;
    }

    public static ZebraFrequency of(final RfidDevice.Frequency frequency) {
        return _frequencies.get(frequency);
    }

}
