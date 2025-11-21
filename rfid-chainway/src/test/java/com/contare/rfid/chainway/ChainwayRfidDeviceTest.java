package com.contare.rfid.chainway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChainwayRfidDeviceTest {

    private final ChainwayRfidDevice device = new ChainwayRfidDevice();

    @Test
    @DisplayName("Check if device manufacturer match")
    public void checkIfManufacturerMatch() {
        assertEquals("chainway", device.manufacturer());
    }

}
