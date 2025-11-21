package com.contare.rfid.chainway;

import com.contare.rfid.NativeLoader;
import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.events.RfidDeviceEvent;
import com.contare.rfid.events.StatusEvent;
import com.contare.rfid.events.TagEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.*;
import com.rscja.deviceapi.ConnectionState;
import com.rscja.deviceapi.entity.Gen2Entity;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHF;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class ChainwayDevice<T extends IUHF> implements RfidDevice {

    protected final Logger logger;

    protected final T uhf;

    protected final int _minPower = 0;
    protected final int _maxPower = 33;
    protected final Set<TagMetadata> _buffer = new HashSet<>();
    protected final Set<String> _uniques = new HashSet<>();

    protected final ExecutorService executor;
    protected volatile Consumer<RfidDeviceEvent> _callback;
    protected volatile Status _status = Status.DISCONNECTED;
    protected boolean isReading = false;

    protected Options opts;

    static {
        try {
            NativeLoader.load("files/libTagReader.so");
            NativeLoader.load("files/UHFAPI.dll");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected ChainwayDevice(final T uhf, final ExecutorService executor, final Class<?> clazz) {
        this.uhf = uhf;
        this.executor = executor;
        this.logger = Logger.getLogger(clazz);
    }

    @Override
    public int getMinPower() {
        return _minPower;
    }

    @Override
    public int getMaxPower() {
        return _maxPower;
    }

    @Override
    public Set<TagMetadata> getBuffer() {
        return Collections.unmodifiableSet(_buffer);
    }

    @Override
    public void clearBuffer() {
        _buffer.clear();
        _uniques.clear();
    }

    public abstract boolean init(final Options opts) throws RfidDeviceException;

    @Override
    public boolean connect(final Options opts) throws RfidDeviceException {
        try {
            this.opts = opts;

            boolean connected = this.init(opts);

            if (connected) {
                uhf.setConnectionStateCallback((state, obj) -> {
                    logger.debugf("Device connection state changed state = %s, obj = %s", state, obj);

                    final Status status = toStatus(state);
                    _status = status;

                    // dispatch status change to user listener
                    executor.execute(() -> {
                        _callback.accept(new StatusEvent(status));
                    });
                });
            }

            return connected;
        } catch (Exception e) {
            throw new RfidDeviceException(e, "Failed to connect to device.");
        }
    }

    @Override
    public void disconnect() {
        // remove callbacks
        uhf.setConnectionStateCallback(null);
        uhf.setInventoryCallback(null);

        // free device resources
        boolean freed = uhf.free();
        if (freed) {
            logger.debugf("Device successfully freed");
        }
    }

    @Override
    public boolean isConnected() {
        return _status == Status.CONNECTED;
    }

    @Override
    public RfidDeviceParams getInventoryParameters() {
        final Gen2Entity entity = uhf.getGen2();
        // TODO: how do we transform Gen2Entity to RfidDeviceParams?
        return null;
    }

    @Override
    public boolean setInventoryParameters(final RfidDeviceParams params) {
        // TODO: how do we transform RfidDeviceParams to Gen2Entity?
        final Gen2Entity entity = null;
        return uhf.setGen2(entity);
    }

    @Override
    public void setCallback(final Consumer<RfidDeviceEvent> callback) {
        _callback = callback;
    }

    @Override
    public boolean startInventory() throws RfidDeviceException {
        if (isReading) {
            throw new RfidDeviceException("Device is already reading.");
        }

        // final Consumer<TagMetadata> callback = Optional.ofNullable(_callback).orElse(_defaultCallback);
        uhf.setInventoryCallback((info) -> {
            try {
                // convert chainway tag info to our common tag object
                final TagMetadata tag = toTagMetadata(info);

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
        });

        boolean started = uhf.startInventoryTag();
        if (started) {
            logger.debugf("Device successfully started inventory");
        }
        isReading = started;
        return started;
    }

    @Override
    public boolean stopInventory() {
        boolean stopped = uhf.stopInventory();
        if (stopped) {
            logger.debugf("Device successfully stopped inventory");
        } else {
            logger.errorf("Device failed to stop inventory");
        }
        return stopped;
    }

    @Override
    public boolean isReading() {
        return isReading;
    }

    @Override
    public RfidDeviceFrequency getFrequency() {
        final int mask = uhf.getFrequencyMode();
        final ChainwayFrequency value = ChainwayFrequency.of(mask);
        return value.toFrequency();
    }

    @Override
    public boolean setFrequency(final RfidDeviceFrequency frequency) {
        final ChainwayFrequency value = ChainwayFrequency.of(frequency);
        return uhf.setFrequencyMode((byte) value.getMask());
    }

    @Override
    public abstract int getPower();

    @Override
    public abstract boolean setPower(final int value);

    @Override
    public boolean setBeep(final boolean enabled) {
        return uhf.setBeep(enabled ? 1 : 0);
    }

    @Override
    public boolean getBeep() {
        final char[] result = uhf.getBeep();
        return result[0] == 1;
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

    /**
     * Get protocol.
     *
     * @return protocol.
     */
    public Protocol getProtocol() {
        final int mask = uhf.getProtocol();
        final Protocol value = Protocol.get(mask);
        if (value == null) {
            throw new UnsupportedOperationException(String.format("Unsupported protocol mask %d", mask));
        }
        return value;
    }

    /**
     * Set protocol.
     *
     * @param value - protocol.
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setProtocol(final Protocol value) {
        return uhf.setProtocol(value.getMask());
    }

    /**
     * Get link parameters
     *
     * @return RFLink
     */
    public RFLink getRFLink() {
        final int mask = uhf.getRFLink();
        final RFLink value = RFLink.get(mask);
        if (value == null) {
            throw new UnsupportedOperationException(String.format("Unsupported RFLink mask %d", mask));
        }
        return value;
    }

    /**
     * Set link parameters
     *
     * @param value - RFLink
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setRFLink(final RFLink value) {
        return uhf.setRFLink(value.getMask());
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

    /**
     * Get continuous wave status.
     *
     * @return true if continuous wave is 'on', false if is 'off'.
     */
    public boolean getContinuousWave() {
        return uhf.getCW() == 1;
    }

    /**
     * Set continuous wave status.
     *
     * @param enabled - true to enable continuous wave, false to disable.
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setContinuousWave(final boolean enabled) {
        final int flag = enabled ? 1 : 0; // 1 - on, 0 - off
        final boolean updated = uhf.setCW(flag);
        if (updated) {
            logger.debugf("Continuous wave set to '%d' = '%s'", flag, (flag == 1) ? "on" : "off");
        }
        return updated;
    }

    /**
     * Switch FastID function
     *
     * @param enabled - true to enable, false to disable
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setFastID(final boolean enabled) {
        return uhf.setFastID(enabled);
    }

    /**
     * Setup frequency Hop.
     *
     * @param value
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setFreeHop(final float value) {
        return uhf.setFreHop(value);
    }

    /**
     * Get the duty cycle.
     *
     * @return
     */
    public void getPwm() {
        final int[] array = uhf.getPwm();
        final int workTime = array[0];
        final int waitTime = array[1]; // return null on failure
    }

    /**
     * Setup the duty cycle.
     *
     * @param workTime - working time
     * @param waitTime - waiting time in milliseconds.
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setPwm(final int workTime, final int waitTime) {
        return uhf.setPwm(workTime, waitTime);
    }

    /**
     * Get UHF module temperature.
     *
     * @return module temperature. -1 means return failure.
     */
    public int getTemperature() {
        return uhf.getTemperature();
    }

    /**
     * Set up a round-robin inventory of tags that meet the filter conditions, and tags that do not meet the filter conditions will not be uploaded.
     * Note: Must be set before cycle count label
     *
     * @param bank - filtered banks (IUHF.Bank_EPC, IUHF.Bank_TID or IUHF.Bank_USER)
     * @param ptr  - filter starting address
     * @param cnt  - filter data length, when filtered data length is 0, it means not filtering.
     * @param data - filter data
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setFilter(final int bank, final int ptr, final int cnt, final String data) {
        final boolean isOk = (bank == IUHF.Bank_EPC || bank == IUHF.Bank_TID || bank == IUHF.Bank_USER);
        if (!isOk) {
            throw new IllegalArgumentException("Filter bank must be a value from 1 to 3");
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Filter data must not be empty");
        }

        if (data.length() * 4 < cnt) {
            throw new IllegalArgumentException("Filter data length must be greater than or equal to filter bit count");
        }
        return uhf.setFilter(bank, ptr, cnt, data);
    }

    /**
     * Write data into tag.
     *
     * @param accessPwd  - access password (4 bytes)
     * @param filterBank - filtered banks (IUHF.Bank_EPC, IUHF.Bank_TID or IUHF.Bank_USER)
     * @param filterPtr  - filter starting address
     * @param filterCnt  - filter data length, when filtered data length is 0, it means not filtering.
     * @param filterData - filter data
     * @param bank       - write banks (IUHF.Bank_RESERVED, IUHF.Bank_EPC, IUHF.Bank_TID or IUHF.Bank_USER)
     * @param ptr        - write starting address
     * @param cnt        - write data length, can not be 0
     * @param writeData  - write data, must be hexadecimal value
     * @return true if operation succeeded, false otherwise.
     */
    public boolean writeData(final String accessPwd,
                             final int filterBank,
                             final int filterPtr,
                             final int filterCnt,
                             final String filterData,
                             final int bank,
                             final int ptr,
                             final int cnt,
                             final String writeData
    ) {
        if (accessPwd == null || accessPwd.isEmpty()) {
            throw new IllegalArgumentException("Filter password must not be empty");
        } else if (accessPwd.length() != 8) {
            throw new IllegalArgumentException("Filter password must be 8 characters long");
        }

        if (cnt <= 0) {
            throw new IllegalArgumentException("Write data length must be greater than 0");
        }

        if (writeData == null || writeData.isEmpty()) {
            throw new IllegalArgumentException("Write data must not be empty");
        }

        if (filterCnt > 0) {
            if (filterData == null || filterData.isEmpty()) {
                throw new IllegalArgumentException("Filter data must not be empty");
            }

            if (filterData.length() * 4 < filterCnt) {
                throw new IllegalArgumentException("Filter data length must be greater than or equal to filter bit count");
            }
            return uhf.writeData(accessPwd, filterBank, filterPtr, filterCnt, filterData, bank, ptr, cnt, writeData);
        }
        return uhf.writeData(accessPwd, bank, ptr, cnt, writeData);
    }

    /**
     * Destroy the label.
     * <p>
     * Example:
     * <p>
     * this.killTag("00000000");
     * this.killTag("00000000", IUHG.Bank_EPC, 0, 0, "00000000000000000000000000000000");
     *
     * @param filterPwd  - password, Default 0x00 0x00 0x00 0x00
     * @param filterBank - filtered bank (IUHF.Bank_EPC, IUHF.Bank_TID or IUHF.Bank_USER)
     * @param filterPtr  - starting address of the filter
     * @param filterCnt  - filter data length, if filter data length is 0, it means no filtering
     * @param filterData - filter data
     * @return true if operation succeeded, false otherwise.
     */
    public boolean killTag(final String filterPwd, final int filterBank, final int filterPtr, final int filterCnt, final String filterData) {
        if (filterPwd == null || filterPwd.isEmpty()) {
            throw new IllegalArgumentException("Filter password must not be empty");
        } else if (filterPwd.length() != 8) {
            throw new IllegalArgumentException("Filter password must be 8 characters long");
        }

        if (filterBank == IUHF.Bank_EPC || filterBank == IUHF.Bank_TID || filterBank == IUHF.Bank_USER) {
            if (filterData == null || filterData.isEmpty()) {
                throw new IllegalArgumentException("Filter data must not be empty");
            }
            if (filterData.length() * 4 < filterCnt) {
                throw new IllegalArgumentException("Filter data length must be greater than or equal to filter bit count");
            }

            return uhf.killTag(filterPwd, filterBank, filterPtr, filterCnt, filterData);
        }

        return uhf.killTag(filterPwd);
    }

    // HELPERS
    private TagMetadata toTagMetadata(final UHFTAGInfo info) {
        final Integer antenna = Integer.parseInt(info.getAnt());
        return new TagMetadata(info.getEPC(), info.getTid(), info.getRssi(), antenna);
    }

    private Status toStatus(final ConnectionState state) {
        switch (state) {
            case CONNECTED:
                return Status.CONNECTED;
            case DISCONNECTED:
                return Status.DISCONNECTED;
            case CONNTCTING:
                return Status.CONNECTING;
            default:
                return Status.UNKNOWN;
        }
    }

}
