package com.contare.rfid.impinj;

import com.contare.rfid.devices.BufferedRfidDevice;
import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.events.RfidDeviceEvent;
import com.contare.rfid.events.TagEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.Options;
import com.contare.rfid.objects.RfidDeviceFrequency;
import com.contare.rfid.objects.RfidDeviceParams;
import com.contare.rfid.objects.TagMetadata;
import com.impinj.octane.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ImpinjDevice extends BufferedRfidDevice implements RfidDevice {

    private final Logger logger = Logger.getLogger(ImpinjDevice.class);

    private final ImpinjReader reader = new ImpinjReader();

    private final ExecutorService executor;
    private Consumer<RfidDeviceEvent> _callback;
    private Options opts;
    private boolean reading = false;

    public ImpinjDevice(final ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public int getMinPower() {
        return 0;
    }

    @Override
    public int getMaxPower() {
        return 0;
    }

    @Override
    public boolean connect(final Options opts) throws RfidDeviceException {
        Objects.requireNonNull(opts, "Options must not be null.");

        this.opts = opts;

        try {
            reader.connect(opts.getIp(), opts.getPort());

            final Settings settings = reader.queryDefaultSettings();
            reader.applySettings(settings);

            return true;
        } catch (OctaneSdkException e) {
            throw new RfidDeviceException(e, "Failed to connect to device.");
        }
    }

    @Override
    public void disconnect() throws RfidDeviceException {
        reader.disconnect();
    }

    @Override
    public boolean isConnected() {
        return reader.isConnected();
    }

    @Override
    public RfidDeviceParams getInventoryParameters() {
        return null;
    }

    @Override
    public boolean setInventoryParameters(RfidDeviceParams params) {
        return false;
    }

    @Override
    public void setCallback(Consumer<RfidDeviceEvent> callback) {
        _callback = callback;
    }

    @Override
    public boolean startInventory() throws RfidDeviceException {
        try {
            if (!isConnected()) {
                throw new RfidDeviceException("Reader is not connected.");
            }

            if (isReading()) {
                throw new RfidDeviceException("Reader is already reading.");
            }

            final Settings settings = reader.querySettings();
            settings.getReport().setIncludeAntennaPortNumber(true);
            settings.getReport().setIncludePeakRssi(true);
            settings.setRfMode(1003);

            final AntennaConfigGroup antennas = settings.getAntennas();

            // TODO: add to options
            final List<Short> enableAntennas = List.of((short) 1, (short) 0, (short) 1, (short) 1);
            // List<Short> antennasNumberEnable = middleware.getAntenasEnable()
            //     .stream()
            //     .map(Integer::shortValue)
            //     .collect(Collectors.toList());

            antennas.forEach(ant -> {
                final short port = ant.getPortNumber();
                ant.setEnabled(enableAntennas.contains(port));
            });

            settings.setAntennas(antennas);

            reader.applySettings(settings);

            reader.setTagReportListener((_reader, tagReport) -> {
                try {
                    final List<Tag> tags = tagReport.getTags();
                    logger.infof("Tag reported: %s", tags.size());

                    final List<TagMetadata> list = tags.stream()
                        .map(v -> toTagMetadata(v))
                        .filter(t -> _uniques.add(t.rfid))
                        .filter(t -> _buffer.add(t))
                        .collect(Collectors.toList());

                    // dispatch tag to user callback
                    executor.execute(() -> {
                        for (TagMetadata row : list) {
                            _callback.accept(new TagEvent(row));
                        }
                    });

                } catch (Exception e) {
                    logger.errorf("Error processing tag metadata: %s", e.getMessage());
                }
            });

            reader.start();

            reading = true;

            return true;
        } catch (OctaneSdkException e) {
            throw new RfidDeviceException(e, "Failed to start inventory.");
        }
    }

    @Override
    public boolean stopInventory() {
        if (!isReading()) {
            logger.warnf("Reader is not reading.");
            return false;
        }

        try {
            reader.stop();
            reading = false;
            return true;
        } catch (OctaneSdkException e) {
            logger.errorf("Failed to stop inventory: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isReading() {
        return reading;
    }

    @Override
    public RfidDeviceFrequency getFrequency() {
        try {
            final Settings settings = reader.querySettings();
            final List<Double> frequencies = settings.getTxFrequenciesInMhz();
            for (Double freq : frequencies) {
                logger.debugf("Frequency: %s", freq);
            }

            // TODO: how to translate Impinj frequency to 'RfidDeviceFrequency'?
            return null;
        } catch (OctaneSdkException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean setFrequency(final RfidDeviceFrequency frequency) {
        try {
            // TODO: how to translate 'RfidDeviceFrequency' to Impinj frequency?
            final List<Double> frequencies = List.of();
            final Settings settings = reader.querySettings();
            settings.setTxFrequenciesInMhz(new ArrayList<>(frequencies));
            return true;
        } catch (OctaneSdkException e) {
            logger.errorf("Failed to set frequency: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public int getPower() {
        try {
            final Settings settings = reader.querySettings();

            final List<Double> list = new ArrayList<>();
            for (AntennaConfig antennaConfig : settings.getAntennas()) {
                double txPowerinDbm = antennaConfig.getTxPowerinDbm();
                list.add(txPowerinDbm);
            }

            final Double power = list.stream().distinct().findFirst()
                .orElseThrow(() -> new RfidDeviceException("Unable to access antenna power."));

            return power.intValue();
        } catch (OctaneSdkException | RfidDeviceException e) {
            return -1;
        }
    }

    @Override
    public boolean setPower(int value) {
        try {
            final Settings settings = reader.querySettings();

            final AntennaConfigGroup antennas = settings.getAntennas();
            for (AntennaConfig antennaConfig : antennas.getAntennaConfigs()) {
                antennaConfig.setTxPowerinDbm(value);
            }

            reader.applySettings(settings);

            return true;
        } catch (OctaneSdkException e) {
            return false;
        }
    }

    @Override
    public boolean getBeep() {
        return false;
    }

    @Override
    public boolean setBeep(boolean enabled) {
        return false;
    }

    @Override
    public boolean setTagFocus(boolean enabled) {
        try {
            final SearchMode searchMode = (enabled) ? SearchMode.TagFocus : SearchMode.DualTarget;
            final int session = (enabled) ? 1 : 2;

            final Settings settings = reader.querySettings();
            settings.setSearchMode(searchMode);
            settings.setSession(session);

            reader.applySettings(settings);

            return true;
        } catch (OctaneSdkException e) {
            return false;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            this.stopInventory();
            this.disconnect();
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to close device.");
        }
    }

    private TagMetadata toTagMetadata(final Tag tag) {
        final String epc = tag.getEpc().toHexString();
        final String tid = tag.getTid().toHexString();
        final String rssi = Double.toString(tag.getPeakRssiInDbm());
        final short antenna = tag.getAntennaPortNumber();
        return new TagMetadata(epc, tid, rssi, (int) antenna);
    }

}
