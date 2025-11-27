package com.contare.rfid;

import com.contare.rfid.chainway.ChainwayR3;
import com.contare.rfid.chainway.ChainwayUR4;
import com.contare.rfid.devices.FakeRfidDevice;
import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.events.BatteryEvent;
import com.contare.rfid.events.ErrorEvent;
import com.contare.rfid.events.StatusEvent;
import com.contare.rfid.events.TagEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.impinj.ImpinjDevice;
import com.contare.rfid.objects.Options;
import com.contare.rfid.objects.RfidDeviceFrequency;
import com.contare.rfid.zebra.ZebraFX7500;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    private static final Runtime runtime = Runtime.getRuntime();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor((r) -> {
        final Thread t = new Thread("main-worker");
        t.setDaemon(true);
        return t;
    });


    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(final String[] args) {
        run();
    }

    private static void run() {
        final Options opts = new Options("none", "none", 0, 4, false);

        final Set<String> epcs = new HashSet<>();

        try (final RfidDevice device = Factory.create(Factory.FAKE, executor)) {
            final boolean connected = device.connect(opts);
            logger.infof("Connected: '%b'", connected);

            device.setCallback((event) -> {
                if (event instanceof TagEvent) {
                    final TagEvent tEvent = (TagEvent) event;
                    logger.infof("Tag: %s", tEvent.getTag());
                    epcs.add(tEvent.getTag().rfid);
                } else if (event instanceof StatusEvent) {
                    final StatusEvent sEvent = (StatusEvent) event;
                    logger.infof("Status: %s", sEvent.getStatus());
                } else if (event instanceof BatteryEvent) {
                    final BatteryEvent bEvent = (BatteryEvent) event;
                    logger.infof("Battery: %d", bEvent.getLevel());
                } else if (event instanceof ErrorEvent) {
                    final ErrorEvent eEvent = (ErrorEvent) event;
                    logger.errorf(eEvent.getCause(), "An unexpected error happened.");
                }
            });

            final RfidDeviceFrequency frequency = RfidDeviceFrequency.BRAZIL;
            boolean fUpdated = device.setFrequency(frequency);
            if (fUpdated) {
                logger.infof("Frequency updated to: '%s'", frequency);
            }

            final int power = 30;
            boolean pUpdated = device.setPower(power);
            if (pUpdated) {
                logger.infof("Power updated to '%d' dbm", power);
            }

            final boolean beep = true;
            final boolean bUpdated = device.setBeep(beep);
            if (bUpdated) {
                logger.infof("Beep changed to '%s'", beep ? "enabled" : "disabled");
            }

            final boolean started = device.startInventory();
            logger.infof("Started: '%b'", started);

            // Add shutdown hook to react to Ctrl+C
            runtime.addShutdownHook(new Thread(() -> {
                logger.info("Shutdown requested (Ctrl+C). Stopping inventory...");
                try {
                    device.stopInventory();
                } catch (Exception ignored) {
                    // ignore
                }
                shutdownLatch.countDown();
            }));

            // Wait here until Ctrl+C
            logger.info("Press Ctrl+C to stop...");
            shutdownLatch.await();

            boolean stopped = device.stopInventory();
            logger.infof("Stopped: '%s'", stopped);

            logger.infof("Unique Tags: %d", epcs.size());
        } catch (RfidDeviceException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Factory {

        public static final String FAKE = "fake";
        public static final String CHAINWAY_R3 = "chainway-r3";
        public static final String CHAINWAY_UR4 = "chainway-ur4";
        public static final String IMPINJ = "impinj";
        public static final String ZEBRA_FX7500 = "zebra-fx7500";

        public static RfidDevice create(final String value, final ExecutorService executor) {
            switch (value) {
                case CHAINWAY_R3:
                    return new ChainwayR3(executor);
                case CHAINWAY_UR4:
                    return new ChainwayUR4(executor);
                case IMPINJ:
                    return new ImpinjDevice(executor);
                case ZEBRA_FX7500:
                    return new ZebraFX7500(executor);
                default:
                    return new FakeRfidDevice();
            }
        }

    }

}
