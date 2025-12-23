package com.contare.rfid.acura;

import com.contare.rfid.devices.RfidDevice;
import com.thingmagic.Reader;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum AcuraFrequency {

    UNITED_STATES(RfidDevice.Frequency.UNITED_STATES, Reader.Region.NA),
    EUROPE(RfidDevice.Frequency.EUROPE, Reader.Region.EU),
    KOREAN(RfidDevice.Frequency.KOREAN, Reader.Region.KR),
    JAPAN(RfidDevice.Frequency.JAPAN, Reader.Region.JP),
    TAIWAN(RfidDevice.Frequency.TAIWAN, Reader.Region.TW),
    VIETNAM(RfidDevice.Frequency.VIETNAM, Reader.Region.VN),
    MALAYSIA(RfidDevice.Frequency.MALAYSIA, Reader.Region.MY),
    RUSSIA(RfidDevice.Frequency.RUSSIA, Reader.Region.RU),
    HONG_KONG(RfidDevice.Frequency.HONG_KONG, Reader.Region.HK),
    BRAZIL(RfidDevice.Frequency.BRAZIL, Reader.Region.OPEN, 250, 902_000, 905_000, 915_000, 927_500),

    NONE(null, Reader.Region.OPEN);

    private static final Map<RfidDevice.Frequency, AcuraFrequency> _frequencies = new HashMap<>();

    static {
        for (AcuraFrequency row : values()) {
            if (row.frequency != null) {
                _frequencies.put(row.frequency, row);
            }
        }
    }

    private final RfidDevice.Frequency frequency;
    private final Reader.Region region;
    private final int step;       // in kHz
    private final int[] table;    // array of frequencies in kHz

    AcuraFrequency(final RfidDevice.Frequency frequency, final Reader.Region region) {
        this(frequency, region, -1);
    }

    AcuraFrequency(final RfidDevice.Frequency frequency, final Reader.Region region, final int step, final int... channels) {
        this.frequency = frequency;
        this.region = region;
        this.step = step;
        this.table = createTable(step, channels);
    }

    private int[] createTable(final int step, final int... channels) {
        final int length = channels.length;
        if (length % 2 != 0) {
            throw new IllegalArgumentException("channels must be pairs: [lower, upper]");
        }

        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < channels.length; i += 2) {
            int lower = channels[i];
            int upper = channels[i + 1];

            int f = lower;
            while (f <= upper) {
                values.add(f);
                f += step;
            }
        }

        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    public RfidDevice.Frequency toFrequency() {
        return frequency;
    }

    public static AcuraFrequency of(final RfidDevice.Frequency frequency) {
        return _frequencies.getOrDefault(frequency, NONE);
    }

}
