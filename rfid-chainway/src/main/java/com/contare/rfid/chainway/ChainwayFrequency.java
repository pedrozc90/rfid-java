package com.contare.rfid.chainway;

import com.contare.rfid.objects.RfidDeviceFrequency;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ChainwayFrequency {

    CHINA_LOWER(0x01, RfidDeviceFrequency.CHINA_LOWER), // China (840 ~ 845 MHz)
    CHINA_UPPER(0x02, RfidDeviceFrequency.CHINA_UPPER), // China (920 ~ 925 MHz)
    EUROPE(0x04, RfidDeviceFrequency.EUROPE), // Europe (865 ~ 868 MHz)
    UNITED_STATES(0x08, RfidDeviceFrequency.UNITED_STATES), // United States (902 ~ 928 MHz)
    KOREAN(0x16, RfidDeviceFrequency.KOREAN), // Korea (917 ~ 923 MHz)
    JAPAN(0x32, RfidDeviceFrequency.JAPAN), // Japan (916.8 ~ 920.8 MHz)
    SOUTH_AFRICA(0x33, RfidDeviceFrequency.SOUTH_AFRICA), // South Africa (915 ~ 919 MHz)
    TAIWAN(0x34, RfidDeviceFrequency.TAIWAN), // Taiwan (920 ~ 928 MHz)
    VIETNAM(0x35, RfidDeviceFrequency.VIETNAM), // Vietnam (918 ~ 923 MHz)
    PERU(0x36, RfidDeviceFrequency.PERU), // Peru (915 ~ 928 MHz)
    RUSSIA(0x37, RfidDeviceFrequency.RUSSIA), // Russia (860 ~ 867.6 MHz)
    MOROCCO(0x80, RfidDeviceFrequency.MOROCCO), // Morocco (914 ~ 921 MHz)
    MALAYSIA(0x3B, RfidDeviceFrequency.MALAYSIA), // Malaysia (919 ~ 923 MHz)
    BRAZIL(0x3C, RfidDeviceFrequency.BRAZIL); // Brazil (902~907.5 MHz)

    // convert "mask" to "ChainwayFrequency"
    private static final Map<Integer, ChainwayFrequency> _masks = new HashMap<>();
    // convert "Frequency" to "ChainwayFrequency"
    private static final Map<RfidDeviceFrequency, ChainwayFrequency> _frequencies = new HashMap<>();

    static {
        for (ChainwayFrequency row : values()) {
            _masks.put(row.mask, row);
            if (row.frequency != null) {
                _frequencies.put(row.frequency, row);
            }
        }
    }

    private final int mask;
    private final RfidDeviceFrequency frequency;

    ChainwayFrequency(final int mask, final RfidDeviceFrequency frequency) {
        this.mask = mask;
        this.frequency = frequency;
    }

    /**
     * Returns the generic Frequency corresponding to this vendor enum.
     */
    public RfidDeviceFrequency toFrequency() {
        return frequency;
    }

    /**
     * Lookup ChainwayFrequency by mask (int). Returns null if not found.
     */
    public static ChainwayFrequency of(final int mask) {
        return _masks.get(mask);
    }

    /**
     * Lookup ChainwayFrequency by Frequency. Returns null if not found.
     */
    public static ChainwayFrequency of(final RfidDeviceFrequency frequency) {
        return _frequencies.get(frequency);
    }

}
