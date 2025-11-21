package com.contare.rfid;

import com.contare.rfid.devices.FakeRfidDevice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FakeRfidDeviceTest {

    @Test
    @DisplayName("EpcGenerator returns a valid SGTIN-96 epc")
    public void EpcGeneratorReturnsValidSGTIN96() {
        final String result = FakeRfidDevice.EpcGenerator.generateSgtin("812345", 6789);
        assertEquals("3074257BF7194E4000001A85", result);
    }

}
