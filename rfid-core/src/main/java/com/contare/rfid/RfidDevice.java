package com.contare.rfid;

import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Set;
import java.util.function.Consumer;

public interface RfidDevice extends AutoCloseable {

    String manufacturer();

    Set<TagMetadata> getBuffer();

    void clearBuffer();

    boolean connect(final Options opts) throws RfidDeviceException;

    void disconnect();

    boolean isConnected();

    boolean isReading();

    void setCallback(final Consumer<TagMetadata> callback);

    boolean startInventory() throws RfidDeviceException;

    boolean stopInventory();

    Frequency getFrequency();

    boolean getFrequency(final Frequency frequency);

    int getPower();

    boolean setPower(final int value);

    boolean setTagFocus(final boolean enabled);

    @Data
    @AllArgsConstructor
    class Options {

        private final String serial; // serial port
        private final String ip;     // network ip address
        private final int port;      // network port

        private final short vendor;
        private final short productId;

    }

    @Getter
    enum Frequency {

        CHINA_LOWER("China (840~845MHz)", 0x01),
        CHINA_UPPER("China (920~925MHz)", 0x02),
        EUROPE("Europe (865~868MHz)", 0x04),
        USA("USA (902~928MHz)", 0x08),
        KOREAN("Korea (917~923MHz)", 0x16),
        JAPAN("Japan (952~953MHz)", 0x32),
        SOUTH_AFRICA("South Africa (915~919MHz)", 0x33),
        TAIWAN("Taiwan (920~928Mhz)", 0x34),
        PERU("Peru (915-928 MHz)", 0x36),
        RUSSIA("Russia (860MHz-867.6MHz)", 0x37),
        BRAZIL("Brazil (905~907.5MHz", 0x3C);

        private final String label;
        private final int chainway;

        Frequency(final String label, final int chainway) {
            this.label = label;
            this.chainway = chainway;
        }

    }

}
