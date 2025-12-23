package com.contare.rfid.objects;

import com.contare.rfid.devices.RfidDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InventoryParamsTest {

    @Test
    public void test() {
        final RfidDevice.Params params = new RfidDevice.Params("0", "x", "y");

        final RfidDevice.Params result = params.withQ("1");
        assertEquals("1", result.getQ());
        assertEquals(params.getX(), result.getX());
        assertEquals(params.getY(), result.getY());
    }

}
