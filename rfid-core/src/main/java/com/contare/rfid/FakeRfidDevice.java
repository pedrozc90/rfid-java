package com.contare.rfid;

import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FakeRfidDevice implements RfidDevice {

    private static final Logger logger = Logger.getLogger(FakeRfidDevice.class);

    private static final List<TagMetadata> _source = new ArrayList<>();
    private static final Set<TagMetadata> _buffer = new HashSet<>();

    static {
        final int max = 10_000;
        for (int i = 0; i < max; i++) {
            _source.add(new TagMetadata(String.format("%016x", i), null, null, 0));
        }
    }

    private Consumer<TagMetadata> callback;
    private Frequency _frequency = Frequency.BRAZIL;
    private int _power = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public String manufacturer() {
        return "fake";
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
        final Random random = new Random();
        executor.execute(() -> {
            final int index = random.nextInt(0, 10_000);
            final TagMetadata tag = _source.get(index);
            boolean added = _buffer.add(tag);
            if (added) {
                callback.accept(tag);
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // ignore
            }
        });
        return true;
    }

    @Override
    public void disconnect() {
        executor.shutdown();
    }

    @Override
    public boolean isConnected() {
        return !executor.isTerminated();
    }

    @Override
    public boolean isReading() {
        return !executor.isShutdown();
    }

    @Override
    public void setCallback(final Consumer<TagMetadata> callback) {
        this.callback = callback;
    }

    @Override
    public boolean startInventory() {
        return true;
    }

    @Override
    public boolean stopInventory() {
        return true;
    }

    @Override
    public Frequency getFrequency() {
        return _frequency;
    }

    @Override
    public boolean getFrequency(final Frequency value) {
        _frequency = value;
        return true;
    }

    @Override
    public int getPower() {
        return _power;
    }

    @Override
    public boolean setPower(int value) {
        _power = value;
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

}
