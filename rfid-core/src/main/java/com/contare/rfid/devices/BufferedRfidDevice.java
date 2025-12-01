package com.contare.rfid.devices;

import com.contare.rfid.objects.TagMetadata;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BufferedRfidDevice implements RfidDevice {

    protected final Set<TagMetadata> _buffer = ConcurrentHashMap.newKeySet();
    protected final Set<String> _uniques = ConcurrentHashMap.newKeySet();

    @Override
    public Set<TagMetadata> getBuffer() {
        return Collections.unmodifiableSet(_buffer);
    }

    @Override
    public void clearBuffer() {
        _buffer.clear();
        _uniques.clear();
    }

}
