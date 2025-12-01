package com.contare.rfid.acura;

import com.contare.rfid.devices.BufferedRfidDevice;
import com.contare.rfid.devices.RfidDevice;

public abstract class AcuraBaseDevice extends BufferedRfidDevice implements RfidDevice {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    private static final int minPower = 0;
    private static final int maxPower = 100;

    @Override
    public int getMinPower() {
        return minPower;
    }

    @Override
    public int getMaxPower() {
        return maxPower;
    }

}
