package com.contare.rfid.devices;

import com.contare.rfid.events.RfidDeviceEvent;
import com.contare.rfid.events.TagEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.Options;
import com.contare.rfid.objects.RfidDeviceFrequency;
import com.contare.rfid.objects.RfidDeviceParams;
import com.contare.rfid.objects.TagMetadata;
import org.jboss.logging.Logger;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FakeRfidDevice implements RfidDevice {

    private static final Logger logger = Logger.getLogger(FakeRfidDevice.class);

    private static final List<TagMetadata> _source = new ArrayList<>();
    private static final Set<TagMetadata> _buffer = new HashSet<>();

    static {
        final int max = 10_000;
        for (int i = 0; i < max; i++) {
            final String rfid = EpcGenerator.generateSgtin("101010", i);
            _source.add(new TagMetadata(rfid, null, null, 0));
        }
    }

    private final int _minPower = 0;
    private final int _maxPower = 100;

    private RfidDeviceParams _params = null;
    private Consumer<RfidDeviceEvent> _callback = (event) -> { /* ignore */ };
    private RfidDeviceFrequency _frequency = RfidDeviceFrequency.BRAZIL;
    private int _power = 0;
    private boolean _beep = true;
    private boolean isConnected = false;
    private boolean isReading = false;

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
    }

    @Override
    public boolean connect(final Options opts) throws RfidDeviceException {
        isConnected = true;
        return true;
    }

    @Override
    public void disconnect() {
        isConnected = false;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public RfidDeviceParams getInventoryParameters() {
        return _params;
    }

    @Override
    public boolean setInventoryParameters(final RfidDeviceParams params) {
        _params = params;
        return true;
    }

    @Override
    public void setCallback(final Consumer<RfidDeviceEvent> callback) {
        _callback = callback;
    }

    @Override
    public boolean startInventory() {
        final int initialDelay = 0;
        final int delay = 100;
        final TimeUnit unit = TimeUnit.MILLISECONDS;

        isReading = true;

        if (executor.isShutdown()) {
            executor = Executors.newSingleThreadScheduledExecutor();
        }

        executor.scheduleWithFixedDelay(() -> {
                try {
                    final int index = ThreadLocalRandom.current().nextInt(0, 10_000);
                    final TagMetadata tag = _source.get(index);
                    boolean added = _buffer.add(tag);
                    if (added) {
                        _callback.accept(new TagEvent(tag));
                    }
                } catch (Exception e) {
                    logger.errorf(e, "Error while generating tag event.");
                }
            },
            initialDelay,
            delay,
            unit
        );

        return true;
    }

    @Override
    public boolean stopInventory() {
        if (executor.isShutdown()) return false;
        executor.shutdown();
        isReading = false;
        logger.debugf("Device successfully stopped inventory");
        return true;
    }

    @Override
    public boolean isReading() {
        return isReading;
    }

    @Override
    public RfidDeviceFrequency getFrequency() {
        return _frequency;
    }

    @Override
    public boolean setFrequency(final RfidDeviceFrequency value) {
        _frequency = value;
        return true;
    }

    @Override
    public int getPower() {
        return _power;
    }

    @Override
    public boolean setPower(int value) {
        if (value < _minPower || value > _maxPower) {
            throw new IllegalArgumentException(String.format("'power' must be between '%d' amd '%d'", _minPower, _maxPower));
        }
        _power = value;
        return true;
    }

    @Override
    public boolean getBeep() {
        return _beep;
    }

    @Override
    public boolean setBeep(boolean enabled) {
        _beep = enabled;
        return true;
    }

    @Override
    public boolean setTagFocus(boolean enabled) {
        return false;
    }

    @Override
    public void close() throws Exception {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }

        if (!executor.isTerminated()) {
            boolean terminated = executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            if (terminated) {
                logger.debugf("Device successfully terminated");
            }
        }

    }

    public static class EpcGenerator {

        /**
         * Simple overload that uses fixed values:
         * filter = 3, partition = 5, companyPrefix = "0614141"
         */
        public static String generateSgtin(String itemReference, long serialNumber) {
            int filter = 3;
            int partition = 5;                 // matches your example (partition value 5)
            String companyPrefix = "0614141";  // preserve leading zero as string

            return generateSgtin(filter, partition, companyPrefix, Integer.parseInt(itemReference), serialNumber);
        }

        /**
         * Full generator that accepts every parameter.
         * companyPrefix must be a decimal string (may contain leading zeros).
         * itemReference is decimal (no leading zeros in int form; digit length is validated against partition).
         */
        private static String generateSgtin(int filter,
                                            int partition,
                                            String companyPrefix,
                                            int itemReference,
                                            long serialNumber) {

            // header for SGTIN-96
            final int HEADER = 0x30; // 8 bits

            // GS1 partition tables (index = partition value)
            final int[] companyBits = { 40, 37, 34, 30, 27, 24, 20 };
            final int[] itemBits = { 4, 7, 10, 14, 17, 20, 24 };
            final int[] cpDigits = { 12, 11, 10, 9, 8, 7, 6 };
            final int[] irDigits = { 2, 3, 4, 5, 6, 7, 8 }; // not used directly but for clarity

            if (partition < 0 || partition > 6) {
                throw new IllegalArgumentException("partition must be 0..6");
            }

            int cpBitLen = companyBits[partition];
            int irBitLen = itemBits[partition];
            int cpDigitLen = cpDigits[partition]; // used to validate string length

            if (companyPrefix == null) throw new IllegalArgumentException("companyPrefix required");
            if (companyPrefix.length() != cpDigitLen) {
                throw new IllegalArgumentException("companyPrefix must have " + cpDigitLen + " digits for partition " + partition);
            }

            // parse numeric values
            long cpValue = Long.parseLong(companyPrefix); // may drop leading zeros numerically but digits were validated
            long irValue = itemReference & ((1L << irBitLen) - 1);

            // serial is 38 bits max
            long serialMask = (1L << 38) - 1;
            long serialValue = serialNumber & serialMask;

            // Build 96-bit value using BigInteger to avoid long overflow
            BigInteger epc = BigInteger.ZERO;

            // shifts: header(8) | filter(3) | partition(3) | company(cpBitLen) | item(irBitLen) | serial(38)
            int shiftHeader = 96 - 8;
            int shiftFilter = shiftHeader - 3;
            int shiftPartition = shiftFilter - 3;
            int shiftCompany = shiftPartition - cpBitLen;
            int shiftItem = shiftCompany - irBitLen;
            // serial occupies the lowest 38 bits (shiftItem - 38 == 0)

            epc = epc.or(BigInteger.valueOf(HEADER & 0xFFL).shiftLeft(shiftHeader));
            epc = epc.or(BigInteger.valueOf(filter & 0x07L).shiftLeft(shiftFilter));
            epc = epc.or(BigInteger.valueOf(partition & 0x07L).shiftLeft(shiftPartition));
            epc = epc.or(BigInteger.valueOf(cpValue).shiftLeft(shiftCompany));
            epc = epc.or(BigInteger.valueOf(irValue).shiftLeft(38));
            epc = epc.or(BigInteger.valueOf(serialValue));

            // return lowercase 24-hex padded (96 bits)
            return String.format("%024x", new BigInteger(epc.toByteArray())).toUpperCase();
        }

    }

}
