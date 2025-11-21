package com.contare;

import com.contare.rfid.objects.TagMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RfidDeviceTest {

    @Test
    @DisplayName("Create TagMetadata instance")
    public void test_TagMetadataInstance() {
        final TagMetadata result = new TagMetadata("3074257bf7194e4000001a85", "0", "0", 0);
        assertEquals("3074257bf7194e4000001a85", result.rfid);
        assertEquals("0", result.tid);
        assertEquals("0", result.rssi);
    }

}
