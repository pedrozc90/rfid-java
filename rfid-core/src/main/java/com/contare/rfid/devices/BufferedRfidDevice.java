package com.contare.rfid.devices;

import com.contare.rfid.objects.TagMetadata;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class BufferedRfidDevice implements RfidDevice {

    protected final Set<TagMetadata> _buffer = new HashSet<>();
    protected final Set<String> _uniques = new HashSet<>();

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
