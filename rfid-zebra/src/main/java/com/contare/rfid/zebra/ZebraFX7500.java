package com.contare.rfid.zebra;

import com.contare.rfid.devices.BufferedRfidDevice;
import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import com.mot.rfid.api3.*;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class ZebraFX7500 extends BufferedRfidDevice implements RfidDevice {

    private static final int minPower = 0;
    private static final int maxPower = 100;

    private final Logger logger = Logger.getLogger(ZebraFX7500.class);

    private RFIDReader reader;
    private final ExecutorService executor;
    private volatile Consumer<RfidDevice.Event> _callback;
    private boolean reading = false;

    public ZebraFX7500(final ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public int getMinPower() {
        return minPower;
    }

    @Override
    public int getMaxPower() {
        return maxPower;
    }

    private TagMetadata toTagMetadata(final Events.ReadEventData data) {
        final String epc = data.tagData.getTagID();
        final String rssi = Short.toString(data.tagData.getPeakRSSI());
        final Integer ant = (int) data.tagData.getAntennaID();
        return new TagMetadata(epc, null, rssi, ant);
    }

    @Override
    public boolean connect(final RfidDevice.Options opts) throws RfidDeviceException {
        Objects.requireNonNull(opts, "Options cannot be null.");

        final String host = opts.getIp();
        Objects.requireNonNull(host, "'host' is required.");

        final int port = Optional.ofNullable(opts.getPort()).orElse(5084);

        reader = new RFIDReader(host, port, 0);
        try {
            reader.connect();
            reader.Actions.purgeTags();

            reader.Events.addEventsListener(new RfidEventsListener() {
                @Override
                public void eventReadNotify(final RfidReadEvents events) {
                    try {
                        final Events.ReadEventData data = events.getReadEventData();
                        final TagMetadata tag = toTagMetadata(data);

                        // check if 'epc' is a new tag
                        if (_uniques.add(tag.rfid)) {
                            // insert it into the buffer
                            if (_buffer.add(tag)) {
                                // dispatch tag to user callback
                                executor.execute(() -> {
                                    _callback.accept(new TagEvent(tag));
                                });
                            } else {
                                logger.warnf("Tag already in buffer: %s", tag);
                            }
                        } else {
                            logger.debugf("Tag already seen: %s", tag);
                        }
                    } catch (Exception e) {
                        logger.errorf("Error processing tag metadata: %s", e.getMessage());
                    }
                }

                @Override
                public void eventStatusNotify(final RfidStatusEvents events) {
                    final Events.StatusEventData data = events.StatusEventData;
                    logger.debugf("Status Event: %s", data);

                    if (data.InventoryStartEventData != null) {
                        logger.debugf("Inventory Start Event: %s", data);
                    }

                    if (data.InventoryStopEventData != null) {
                        logger.debugf("Inventory Stop Event: %s", data);
                    }

                    if (data.DisconnectionEventData != null) {
                        executor.execute(() -> {
                            _callback.accept(new StatusEvent(RfidDevice.Status.DISCONNECTED));
                        });
                    }
                }
            });

            reader.Events.setInventoryStartEvent(true);
            reader.Events.setInventoryStopEvent(true);
            reader.Events.setAccessStartEvent(true);
            reader.Events.setAccessStopEvent(true);
            reader.Events.setTagReadEvent(true); // enables tag read notification. if this is set to false, no tag read notification will be send
            reader.Events.setAntennaEvent(true);
            reader.Events.setBufferFullEvent(true);
            reader.Events.setBufferFullWarningEvent(true);
            reader.Events.setGPIEvent(true);
            reader.Events.setReaderDisconnectEvent(true);
            // tem um bug da desgraça com essa flag, na documentação fala que se true ele manda os dados do epc no
            // listener, porem o comportamento dela na pratica é quando estiver true o listener não esta sendo chamado
            // e o buffer fica cheio e ferra tudo quando false, pelo menos o listener é chamado quando tem algo no buffer,
            // e podemos chamar as tags manualmente do buffer.
            reader.Events.setAttachTagDataWithReadEvent(false);

            // EPC-ID ja vem por padrão, documentação fala que se a gente definir quais campos vem,
            // a leitura pode ser mais rapida e aparecer menos tag repitida na leitura
            final TAG_FIELD[] fields = new TAG_FIELD[2];
            fields[0] = TAG_FIELD.PEAK_RSSI;
            fields[1] = TAG_FIELD.ANTENNA_ID;

            final TagStorageSettings settings = reader.Config.getTagStorageSettings();
            settings.discardTagsOnInventoryStop(true);
            settings.enableAccessReports(true);
            settings.setTagFields(fields);

            reader.Config.setTagStorageSettings(settings);

            final TRACE_LEVEL traceLevel = opts.isVerbose() ? TRACE_LEVEL.TRACE_LEVEL_VERBOSE : TRACE_LEVEL.TRACE_LEVEL_OFF;
            reader.Config.setTraceLevel(traceLevel);

            return true;
        } catch (OperationFailureException | InvalidUsageException e) {
            throw new RfidDeviceException(e, "Failed to connect to device.");
        }
    }

    @Override
    public void disconnect() throws RfidDeviceException {
        try {
            if (reader != null) {
                reader.disconnect();
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            throw new RfidDeviceException(e, "Failed to disconnect device");
        }
    }

    @Override
    public boolean isConnected() {
        return false;
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
        if (reader == null) {
            throw new RfidDeviceException("Reader not initialized.");
        }

        if (reading) {
            throw new RfidDeviceException("Reader is already reading.");
        }

        try {
            reader.Actions.Inventory.perform();
            reading = true;
            return true;
        } catch (InvalidUsageException | OperationFailureException e) {
            throw new RfidDeviceException(e, "Failed to start inventory.");
        }
    }

    @Override
    public boolean stopInventory() {
        try {
            reader.Actions.Inventory.stop();
            reader.Actions.purgeTags();
            reading = false;
            return true;
        } catch (InvalidUsageException | OperationFailureException e) {
            // throw new RfidDeviceException(e, "Failed to stop inventory.");
            return false;
        }
    }

    @Override
    public boolean isReading() {
        return reading;
    }

    @Override
    public boolean killTag(final String rfid, final String password) throws RfidDeviceException {
        try {
            // parse password as unsigned HEX (recommended)
            long value = Long.parseUnsignedLong(password, 16);
            logger.debugf("Kill password (HEX): '%s', (DEC): '%d'", password.toUpperCase(), value);

            final TagAccess.KillAccessParams params = reader.Actions.TagAccess.new KillAccessParams(value);

            final short[] antennas = reader.Config.Antennas.getAvailableAntennas();

            reader.Actions.TagAccess.killWait(rfid, params, new AntennaInfo(antennas));
            logger.debugf("Tag '%s' killed.", rfid);

            return true;
        } catch (InvalidUsageException e) {
            throw new RfidDeviceException(e);
        } catch (OperationFailureException e) {
            throw new RfidDeviceException(e, "Failed to kill tag %s", rfid);
        }
    }

    @Override
    public RfidDevice.Frequency getFrequency() {
        final int length = reader.ReaderCapabilities.RFModes.Length();
        for (int index = 0; index < length; index++) {
            final RFModeTable tableInfo = reader.ReaderCapabilities.RFModes.getRFModeTableInfo(index);
            logger.debugf("RFModeTable[%d]: %s", index, tableInfo);
            // logger.debugf("", tableInfo.getProtocolID());
            // logger.debugf("", tableInfo.getRFModeTableEntryInfo());
        }

        try {
            final Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig((short) 1);
            final long index = config.getrfModeTableIndex();

            logger.debugf("Frequency Index = '%d'", index);

            // TODO: how do i know the frequency?
        } catch (InvalidUsageException e) {
            logger.errorf(e, "Invalid antenna index.");
        } catch (OperationFailureException e) {
            logger.errorf(e, "Failed to get frequency.");
        }
        return null;
    }

    @Override
    public boolean setFrequency(final RfidDevice.Frequency frequency) {
        try {
            final ZebraFrequency freq = ZebraFrequency.of(frequency);
            if (freq == null) {
                throw new IllegalArgumentException("Frequency " + frequency.getLabel() + " is not supported.");
            }

            final Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig((short) 1);
            config.setrfModeTableIndex(freq.getIndex());

            final short[] antennas = reader.Config.Antennas.getAvailableAntennas();
            for (int ant : antennas) {
                reader.Config.Antennas.setAntennaRfConfig(ant, config);
            }

            return true;
        } catch (InvalidUsageException e) {
            logger.errorf(e, "Failed to get frequency.");
        } catch (OperationFailureException e) {
            logger.errorf(e, "Failed to set frequency.");
        }
        return false;
    }

    @Override
    public int getPower() {
        try {
            final int antennnaIndex = 1;
            // final short[] antennas = reader.Config.Antennas.getAvailableAntennas();
            final Antennas.Config config = reader.Config.Antennas.getAntennaConfig(antennnaIndex);
            final short powerIndex = config.getTransmitPowerIndex();
            final int[] powerLevelValues = reader.ReaderCapabilities.getTransmitPowerLevelValues();
            final int powerLevelValue = powerLevelValues[powerIndex];
            return toPowerDbm(powerLevelValue);
        } catch (InvalidUsageException | OperationFailureException e) {
            return -1;
        }
    }

    @Override
    public boolean setPower(final int value) {
        if (reader == null) return false;

        if (value < minPower || value > maxPower) {
            throw new IllegalArgumentException("Power value must be between " + minPower + " and " + maxPower + " dbm.");
        }

        try {
            final short powerIndex = toPowerIndex(value);
            if (powerIndex == -1) {
                throw new IllegalArgumentException(String.format("Power value '%d' not supported.", value));
            }

            // não consegui saber qual esta ativa ou não, por isso eu pego a configuração de cada uma que o leitor aceita e mando a mesma potencia.
            final short[] antennas = reader.Config.Antennas.getAvailableAntennas();
            for (int ant : antennas) {
                final Antennas.Config config = reader.Config.Antennas.getAntennaConfig(ant);
                config.setTransmitPowerIndex(powerIndex);
                reader.Config.Antennas.setAntennaConfig(ant, config);
            }

            return true;
        } catch (InvalidUsageException | OperationFailureException e) {
            return false;
        }
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
        final short[] antennas = reader.Config.Antennas.getAvailableAntennas();
        for (int ant : antennas) {
            try {
                final Antennas.AntennaRfConfig rfConfig = reader.Config.Antennas.getAntennaRfConfig(ant);
                rfConfig.setrfModeTableIndex(enabled ? 23 : 1);

                final Antennas.SingulationControl singulation = reader.Config.Antennas.getSingulationControl(ant);
                singulation.setTagPopulation(enabled ? (short) 100 : (short) 300);  // 100 para Tag Focus, 300 para desativar

                reader.Config.Antennas.setAntennaRfConfig(ant, rfConfig);
                reader.Config.Antennas.setSingulationControl(ant, singulation);
            } catch (InvalidUsageException | OperationFailureException e) {
                return false;
            }
        }
        return true;
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

    // HELPERS
    private int toPowerDbm(final int index) {
        return index / 100;
    }

    private short toPowerIndex(final int dbm) {
        final short value = (short) (dbm * 100);

        final int[] powerLevels = reader.ReaderCapabilities.getTransmitPowerLevelValues();
        for (short i = 0; i < powerLevels.length; i++) {
            if (powerLevels[i] == value) {
                return i;
            }
        }

        return -1;
    }

}
