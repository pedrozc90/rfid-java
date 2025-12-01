package com.contare.rfid.impinj;

import com.contare.rfid.objects.RfidDeviceFrequency;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum ImpinjFrequency {

    BRAZIL(RfidDeviceFrequency.BRAZIL, 902.0, 905.0, 915.0, 927.5);

    private static final Map<RfidDeviceFrequency, ImpinjFrequency> _frequencies = new HashMap<>();

    static {
        for (ImpinjFrequency row : values()) {
            _frequencies.put(row.frequency, row);
        }
    }

    private final RfidDeviceFrequency frequency;
    private final List<Double> array;

    ImpinjFrequency(final RfidDeviceFrequency frequency, final Double... array) {
        this.frequency = frequency;
        this.array = Arrays.asList(array);
    }

    public static ImpinjFrequency of(final RfidDeviceFrequency frequency) {
        return _frequencies.get(frequency);
    }

}
