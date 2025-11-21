package com.contare.rfid.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InventoryParamsTest {

    @Test
    public void test() {
        final RfidDeviceParams params = new RfidDeviceParams("0", "x", "y");

        final RfidDeviceParams result = params.withQ("1");
        assertEquals("1", result.getQ());
        assertEquals(params.getX(), result.getX());
        assertEquals(params.getY(), result.getY());
    }

}
