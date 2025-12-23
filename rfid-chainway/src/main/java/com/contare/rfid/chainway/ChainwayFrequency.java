package com.contare.rfid.chainway;

import com.contare.rfid.devices.RfidDevice;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ChainwayFrequency {

    CHINA_LOWER(0x01, RfidDevice.Frequency.CHINA_LOWER), // China (840 ~ 845 MHz)
    CHINA_UPPER(0x02, RfidDevice.Frequency.CHINA_UPPER), // China (920 ~ 925 MHz)
    EUROPE(0x04, RfidDevice.Frequency.EUROPE), // Europe (865 ~ 868 MHz)
    UNITED_STATES(0x08, RfidDevice.Frequency.UNITED_STATES), // United States (902 ~ 928 MHz)
    KOREAN(0x16, RfidDevice.Frequency.KOREAN), // Korea (917 ~ 923 MHz)
    JAPAN(0x32, RfidDevice.Frequency.JAPAN), // Japan (916.8 ~ 920.8 MHz)
    SOUTH_AFRICA(0x33, RfidDevice.Frequency.SOUTH_AFRICA), // South Africa (915 ~ 919 MHz)
    TAIWAN(0x34, RfidDevice.Frequency.TAIWAN), // Taiwan (920 ~ 928 MHz)
    VIETNAM(0x35, RfidDevice.Frequency.VIETNAM), // Vietnam (918 ~ 923 MHz)
    PERU(0x36, RfidDevice.Frequency.PERU), // Peru (915 ~ 928 MHz)
    RUSSIA(0x37, RfidDevice.Frequency.RUSSIA), // Russia (860 ~ 867.6 MHz)
    MOROCCO(0x80, RfidDevice.Frequency.MOROCCO), // Morocco (914 ~ 921 MHz)
    MALAYSIA(0x3B, RfidDevice.Frequency.MALAYSIA), // Malaysia (919 ~ 923 MHz)
    BRAZIL(0x3C, RfidDevice.Frequency.BRAZIL); // Brazil (902~907.5 MHz)

    // convert "mask" to "ChainwayFrequency"
    private static final Map<Integer, ChainwayFrequency> _masks = new HashMap<>();
    // convert "Frequency" to "ChainwayFrequency"
    private static final Map<RfidDevice.Frequency, ChainwayFrequency> _frequencies = new HashMap<>();

    static {
        for (ChainwayFrequency row : values()) {
            _masks.put(row.mask, row);
            if (row.frequency != null) {
                _frequencies.put(row.frequency, row);
            }
        }
    }

    private final int mask;
    private final RfidDevice.Frequency frequency;

    ChainwayFrequency(final int mask, final RfidDevice.Frequency frequency) {
        this.mask = mask;
        this.frequency = frequency;
    }

    /**
     * Returns the generic Frequency corresponding to this vendor enum.
     */
    public RfidDevice.Frequency toFrequency() {
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
    public static ChainwayFrequency of(final RfidDevice.Frequency frequency) {
        return _frequencies.get(frequency);
    }

}
