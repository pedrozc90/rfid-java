package com.contare.rfid.zebra;

import com.contare.rfid.devices.BufferedRfidDevice;
import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.events.RfidDeviceEvent;
import com.contare.rfid.events.StatusEvent;
import com.contare.rfid.events.TagEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.*;
import com.mot.rfid.api3.*;
import org.jboss.logging.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class ZebraFX7500 extends BufferedRfidDevice implements RfidDevice {

    private final Logger logger = Logger.getLogger(ZebraFX7500.class);

    private RFIDReader reader;
    private final ExecutorService executor;
    private volatile Consumer<RfidDeviceEvent> _callback;
    private boolean reading = false;

    public ZebraFX7500(final ExecutorService executor) {
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

    private TagMetadata toTagMetadata(final Events.ReadEventData data) {
        final String epc = data.tagData.getTagID();
        final String rssi = Short.toString(data.tagData.getPeakRSSI());
        final Integer ant = (int) data.tagData.getAntennaID();
        return new TagMetadata(epc, null, rssi, ant);
    }

    @Override
    public boolean connect(final Options opts) throws RfidDeviceException {
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
                            _callback.accept(new StatusEvent(Status.DISCONNECTED));
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
    public RfidDeviceParams getInventoryParameters() {
        return null;
    }

    @Override
    public boolean setInventoryParameters(final RfidDeviceParams params) {
        return false;
    }

    @Override
    public void setCallback(final Consumer<RfidDeviceEvent> callback) {
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
    public RfidDeviceFrequency getFrequency() {
        return null;
    }

    @Override
    public boolean setFrequency(final RfidDeviceFrequency frequency) {
        return false;
    }

    @Override
    public int getPower() {
        try {
            final int antennnaIndex = 1;
            // short[] availableAntennas = reader.Config.Antennas.getAvailableAntennas();
            final Antennas.Config config = reader.Config.Antennas.getAntennaConfig(antennnaIndex);
            final short powerIndex = config.getTransmitPowerIndex();
            final int[] powerLevelValues = reader.ReaderCapabilities.getTransmitPowerLevelValues();
            final int powerLevelValue = powerLevelValues[powerIndex];
            return toPowerDbm(powerLevelValue);
        } catch (InvalidUsageException | OperationFailureException e) {
            return -1;
        }
    }

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

    @Override
    public boolean setPower(final int value) {
        if (reader == null) return false;

        try {
            final short powerIndex = toPowerIndex(value);
            if (powerIndex == -1) {
                throw new IllegalArgumentException("Power value not supported.");
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

}
