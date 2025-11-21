package com.contare.rfid.chainway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
public class ChainwayUR4Test extends ChainwayTest<ChainwayUR4> {

    @BeforeEach
    public void setUp() {
        device = new ChainwayUR4(executor);
    }

    @Test
    @DisplayName("Instantiate device")
    public void instantiateDevice() {
        assertNotNull(device);
    }

}
