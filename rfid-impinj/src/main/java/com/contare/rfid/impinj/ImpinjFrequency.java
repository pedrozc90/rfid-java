package com.contare.rfid.impinj;

import com.contare.rfid.devices.RfidDevice;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum ImpinjFrequency {

    BRAZIL(RfidDevice.Frequency.BRAZIL, 902.0, 905.0, 915.0, 927.5);

    private static final Map<RfidDevice.Frequency, ImpinjFrequency> _frequencies = new HashMap<>();

    static {
        for (ImpinjFrequency row : values()) {
            _frequencies.put(row.frequency, row);
        }
    }

    private final RfidDevice.Frequency frequency;
    private final List<Double> array;

    ImpinjFrequency(final RfidDevice.Frequency frequency, final Double... array) {
        this.frequency = frequency;
        this.array = Arrays.asList(array);
    }

    public static ImpinjFrequency of(final RfidDevice.Frequency frequency) {
        return _frequencies.get(frequency);
    }

}
