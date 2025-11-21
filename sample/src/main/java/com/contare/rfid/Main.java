package com.contare.rfid;

import com.contare.rfid.chainway.ChainwayRfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    private static final RfidDevice device = Factory.create("fake");

    public static void main(final String[] args) {
        final RfidDevice.Options opts = new RfidDevice.Options("none", "none", 0, (short) 0, (short) 0);

        final Set<String> uniques = new HashSet<>();

        try {
            final boolean connected = device.connect(opts);
            logger.infof("Connected: %s", connected);

            device.setCallback((tag) -> {
                if (uniques.add(tag.rfid)) {
                    logger.infof("Tag: %s", tag);
                }
            });

            final boolean started = device.startInventory();
            logger.infof("Started: %s", started);

            Thread.sleep(5_000);

            boolean stopped = device.stopInventory();
            logger.infof("Stopped: %s", stopped);

            logger.infof("Unique Tags: %d", uniques.size());
        } catch (RfidDeviceException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Factory {

        public static RfidDevice create(final String value) {
            switch (value) {
                case "chainway":
                    return new ChainwayRfidDevice();
                default:
                    return new FakeRfidDevice();
            }
        }

    }

}