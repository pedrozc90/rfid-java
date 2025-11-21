package com.contare.rfid.chainway;

import com.contare.rfid.objects.RfidDeviceFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChainwayTest<T extends ChainwayDevice<?>> {

    protected static final ExecutorService executor = Executors.newSingleThreadExecutor((r) -> {
        final Thread t = new Thread("chainway-worker");
        t.setDaemon(true);
        return t;
    });

    protected T device;

    @ParameterizedTest
    @EnumSource(value = RfidDeviceFrequency.class)
    public void testSetFrequency(final RfidDeviceFrequency frequency) {
        final boolean updated = device.setFrequency(frequency);
        assertTrue(updated);
    }

    @Test
    public void SetPower_MinValue() {
        final int value = device.getMinPower();
        final boolean updated = device.setPower(value);
        assertTrue(updated, "Failed to set power to min value (" + value + ")");
    }

    @Test
    public void SetPower_MaxValue() {
        final int value = device.getMaxPower();
        final boolean updated = device.setPower(value);
        assertTrue(updated, "Failed to set power to max value (" + value + ")");
    }

}
