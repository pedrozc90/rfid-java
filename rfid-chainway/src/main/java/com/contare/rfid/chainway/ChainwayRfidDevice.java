package com.contare.rfid.chainway;

import com.contare.rfid.RfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import com.rscja.deviceapi.RFIDWithUHFNetworkUR4;
import com.rscja.deviceapi.entity.AntennaNameEnum;
import lombok.Getter;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ChainwayRfidDevice implements RfidDevice {

    private static final Logger logger = Logger.getLogger(ChainwayRfidDevice.class);

    private final RFIDWithUHFNetworkUR4 uhf = new RFIDWithUHFNetworkUR4();
    private final Set<TagMetadata> buffer = new HashSet<>();
    private final Set<String> uniques = new HashSet<>();

    private boolean isConnected = false;
    private boolean isReading = false;

    @Override
    public String manufacturer() {
        return "chainway";
    }

    @Override
    public Set<TagMetadata> getBuffer() {
        return Collections.unmodifiableSet(buffer);
    }

    @Override
    public void clearBuffer() {
        buffer.clear();
    }

    @Override
    public boolean connect(final Options opts) throws RfidDeviceException {
        final String ip = opts.getIp();
        final int port = opts.getPort();
        boolean connected = uhf.init(ip, port);
        if (connected) {
            logger.debugf("Chainway RFID device successfully connected to %s:%d", ip, port);
        } else {
            logger.errorf("Chainway RFID device failed to connect to %s:%d", ip, port);
        }

        isConnected = connected;

        return connected;
    }

    @Override
    public void disconnect() {
        boolean freed = uhf.free();
        if (freed) {
            logger.debugf("Chainway RFID device successfully freed");
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public boolean isReading() {
        return isReading;
    }

    @Override
    public void setCallback(final Consumer<TagMetadata> callback) {
        uhf.setInventoryCallback((info) -> {
            try {
                final Integer antenna = Integer.parseInt(info.getAnt());
                final TagMetadata tag = new TagMetadata(info.getEPC(), info.getTid(), info.getRssi(), antenna);
                callback.accept(tag);
            } catch (Exception e) {
                logger.errorf("Error processing tag metadata: %s", e.getMessage());
            }
        });
    }

    @Override
    public boolean startInventory() throws RfidDeviceException {
        if (isReading) {
            throw new RfidDeviceException("Device is already reading.");
        }

        boolean started = uhf.startInventoryTag();
        if (started) {
            logger.debugf("Chainway RFID device successfully started inventory");
        }
        isReading = started;
        return started;
    }

    @Override
    public boolean stopInventory() {
        boolean stopped = uhf.stopInventory();
        if (stopped) {
            logger.debugf("Chainway RFID device successfully stopped inventory");
        } else {
            logger.errorf("Chainway RFID device failed to stop inventory");
        }
        return stopped;
    }

    @Override
    public Frequency getFrequency() {
        int value = uhf.getFrequencyMode();
        return null;
    }

    @Override
    public boolean getFrequency(final Frequency frequency) {
        return uhf.setFrequencyMode((byte) frequency.getChainway());
    }

    @Override
    public int getPower() {
        return uhf.getPower(AntennaNameEnum.ANT1);
    }

    @Override
    public boolean setPower(int value) {
        boolean updated = uhf.setPower(AntennaNameEnum.ANT1, value);
        return updated;
    }

    @Override
    public boolean setTagFocus(boolean enabled) {
        return uhf.setTagFocus(enabled);
    }

    @Override
    public void close() throws Exception {
        this.stopInventory();
        this.disconnect();
    }

    // OTHERS
    @Getter
    public enum Protocol {
        ISO18000_6C("ISO18000-6C", 0),
        GB_T_29768("GB/T 29768", 1),
        GJB_7377_1("GJB 7377.1", 2),
        ISO18000_6B("ISO18000-6B", 3);

        private final String label;
        private final int index;

        Protocol(final String label, final int index) {
            this.label = label;
            this.index = index;
        }

    }

    @Getter
    public enum RFLink {
        DSB_ASK_40MHZ("DSB_ASK/FM0/40KH", 0),
        PR_ASK_250MHZ("PR_ASK/Miller4/250KHz", 1),
        PR_ASK_300MHZ("PR_ASK/Miller4/300KHz", 2),
        DSB_ASK_400MHZ("DSB_ASK/FM0/400KHz", 3);

        private final String label;
        private final int index;

        RFLink(final String label, final int index) {
            this.label = label;
            this.index = index;
        }
    }

    public boolean setProtocol(final Protocol value) {
        return uhf.setProtocol(value.index);
    }

    public boolean setLink(final RFLink value) {
        return uhf.setRFLink(value.index);
    }

    public void setMode(final boolean epc, final boolean tid, final boolean user) {
        // EPC + TID + USER
        if (epc && tid && user) {
            uhf.setEPCAndTIDUserMode(1, 1);
        }
        // EPC + TID
        else if (epc && tid) {
            uhf.setEPCAndTIDMode();
        }
        // EPC
        uhf.setEPCMode();
    }

}
