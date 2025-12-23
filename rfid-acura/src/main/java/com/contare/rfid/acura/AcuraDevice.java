package com.contare.rfid.acura;

import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import com.thingmagic.*;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class AcuraDevice extends AcuraBaseDevice {

    private static final String OS = System.getProperty("os.name").toLowerCase();

    private final Logger logger = Logger.getLogger(AcuraDevice.class.getName());

    private volatile Reader reader;
    private RfidDevice.Options opts;

    private final Executor executor;
    private volatile Consumer<RfidDevice.Event> _callback;
    private volatile ReadListener listener;
    private boolean connected = false;
    private boolean reading = false;

    public AcuraDevice(final Executor executor) {
        this.executor = executor;
    }

    @Override
    public boolean connect(final RfidDevice.Options opts) throws RfidDeviceException {
        Objects.requireNonNull(opts, "Options must not be null");

        final String serialPort = opts.getSerial();
        Objects.requireNonNull(serialPort, "serial port must not be null");

        this.opts = opts;

        final String arg = (OS.contains("linux"))
            ? String.format("tmr:///dev/%s", serialPort)
            : String.format("tmr:///%s", serialPort);

        try {
            reader = Reader.create(arg);
            reader.connect();
            connected = true;
            return true;
        } catch (ReaderException e) {
            throw new RfidDeviceException(e, "Failed to open reader on serial port %s", arg);
        }
    }

    @Override
    public void disconnect() throws RfidDeviceException {
        if (reader != null) {
            if (!isConnected()) {
                throw new RfidDeviceException("Device is not connected");
            }
            reader.destroy();
            connected = false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public RfidDevice.Params getInventoryParameters() {
        return null;
    }

    @Override
    public boolean setInventoryParameters(final RfidDevice.Params params) {
        return false;
    }

    @Override
    public void setCallback(final Consumer<RfidDevice.Event> callback) {
        _callback = callback;
    }

    @Override
    public boolean startInventory() throws RfidDeviceException {
        if (!connected) {
            throw new RfidDeviceException("Device is not connected.");
        }

        if (reading) {
            throw new RfidDeviceException("Device is already reading.");
        }

        try {
            // connect all antennas
            final int[] antennas = (int[]) reader.paramGet(TMConstants.TMR_PARAM_ANTENNA_CONNECTEDPORTLIST);
            final SimpleReadPlan plan = new SimpleReadPlan(antennas, TagProtocol.GEN2, null, null, 1_000);
            reader.paramSet(TMConstants.TMR_PARAM_READ_PLAN, plan);

            // Create and add tag listener
            listener = (_reader, data) -> {
                final String rfid = data.getTag().epcString();
                if (_uniques.add(rfid)) {
                    final String rssi = Integer.toString(data.getRssi());
                    final Integer antenna = data.getAntenna();
                    final TagMetadata tag = new TagMetadata(rfid, null, rssi, antenna);
                    if (_buffer.add(tag)) {
                        executor.execute(() -> {
                            _callback.accept(new TagEvent(tag));
                        });
                    }
                }
            };

            reader.addReadListener(listener);

            reader.addStatusListener(new StatusListener() {
                @Override
                public void statusMessage(final Reader _reader, final SerialReader.StatusReport[] reports) {
                    logger.debugf("Status reports: %d", reports.length);
                }
            });

            // search for tags in the background
            reader.startReading();

            reading = true;

            return true;
        } catch (ReaderException e) {
            throw new RfidDeviceException(e);
        }
    }

    @Override
    public boolean stopInventory() {
        try {
            if (!isConnected()) {
                throw new RfidDeviceException("Device is not connected.");
            }

            if (!isReading()) {
                throw new RfidDeviceException("Device is not reading.");
            }

            if (reader != null) {
                reader.stopReading();
                reader.removeReadListener(listener);
            }

            reading = false;

            return true;
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to stop inventory.");
        }
        return false;
    }

    @Override
    public boolean isReading() {
        return reading;
    }

    @Override
    public boolean killTag(final String rfid, final String password) throws RfidDeviceException {
        try {
            final TagData filter = new TagData(rfid);

            // Kill password (must be 32 bits)
            // Example: 0x00000000 — many tags will NOT allow kill with this
            final int value = Integer.parseUnsignedInt(password, 16);

            final Gen2.Kill operation = new Gen2.Kill(value);

            final Object result = reader.executeTagOp(operation, filter);
            logger.debugf("Kill result: %s", result);

            return true;
        } catch (ReaderException e) {
            throw new RfidDeviceException(e);
        }
    }

    @Override
    public RfidDevice.Frequency getFrequency() {
        try {
            if (reader != null) {
                if (connected) {
                    final Object minFrequency = reader.paramGet(TMConstants.TMR_PARAM_REGION_MINIMUM_FREQUENCY);
                    logger.debugf("minimum frequency = '%s'", minFrequency);

                    final int quantizationStep = (int) reader.paramGet(TMConstants.TMR_PARAM_REGION_QUANTIZATION_STEP);
                    logger.debugf("quantization step = '%d'", quantizationStep);

                    final int[] hopTable = (int[]) reader.paramGet(TMConstants.TMR_PARAM_REGION_HOPTABLE);
                    logger.debugf("hop table = '%s'", Arrays.toString(hopTable));

                    final int[] supportedRegions = (int[]) reader.paramGet(TMConstants.TMR_PARAM_REGION_SUPPORTEDREGIONS);
                    logger.debugf("supported regions = '%s'", supportedRegions);

                    final Reader.Region region = (Reader.Region) reader.paramGet(TMConstants.TMR_PARAM_REGION_ID);
                    logger.debugf("region = '%s'", region);
                }
            }
        } catch (ReaderException e) {
            logger.errorf(e, "Failed to read frequency.");
        }
        return null;
    }

    // TODO: Not sure if works
    @Override
    public boolean setFrequency(final RfidDevice.Frequency frequency) {
        final AcuraFrequency freq = AcuraFrequency.of(frequency);

        try {
            if (reader != null) {
                final Reader.Region region = freq.getRegion();
                reader.paramSet(TMConstants.TMR_PARAM_REGION_ID, region);

                final int step = freq.getStep();
                if (step > 0) {
                    reader.paramSet(TMConstants.TMR_PARAM_REGION_QUANTIZATION_STEP, step);
                }

                final int[] table = freq.getTable();
                if (table != null) {
                    reader.paramSet(TMConstants.TMR_PARAM_REGION_HOPTABLE, table);
                }

                return true;
            }
        } catch (ReaderException e) {
            logger.errorf(e, "Failed to set frequency.");
        }

        return false;
    }

    @Override
    public int getPower() {
        try {
            if (reader != null) {
                if (connected) {
                    final Object maxpower = reader.paramGet(TMConstants.TMR_PARAM_RADIO_POWERMAX);
                    final Object minpower = reader.paramGet(TMConstants.TMR_PARAM_RADIO_POWERMIN);
                    logger.debugf("maxpower = '%s', minpower = '%s'", maxpower, minpower);

                    final int value = (int) reader.paramGet(TMConstants.TMR_PARAM_RADIO_READPOWER);
                    return (value / 100);
                }
            }
        } catch (ReaderException e) {
            logger.errorf(e, "Failed to obtain reader power value.");
        }
        return -1;
    }

    @Override
    public boolean setPower(final int value) {
        try {
            if (reader != null) {
                if (connected) {
                    reader.paramSet(TMConstants.TMR_PARAM_RADIO_READPOWER, value * 100);
                    return true;
                }
            }
        } catch (ReaderException e) {
            logger.errorf(e, "Failed to set reader power value.");
        }
        return false;
    }

    @Override
    public boolean getBeep() {
        return false;
    }

    @Override
    public boolean setBeep(final boolean enabled) {
        return false;
    }

    @Override
    public boolean setTagFocus(final boolean enabled) {
        throw new UnsupportedOperationException("Device do not support tag focus.");
    }

    @Override
    public void close() throws Exception {
        try {
            stopInventory();
            disconnect();
        } catch (RfidDeviceException e) {
            logger.errorf(e, "Failed to close device.");
        }
    }

}
