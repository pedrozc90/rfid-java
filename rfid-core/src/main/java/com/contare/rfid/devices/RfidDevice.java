package com.contare.rfid.devices;

import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.TagMetadata;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;
import java.util.function.Consumer;

public interface RfidDevice extends AutoCloseable {

    /**
     * Returns the minimum power level supported by this device.
     *
     * @return the minimum power level supported by this device.
     */
    int getMinPower();

    /**
     * Returns the maximum power level supported by this device.
     *
     * @return the maximum power level supported by this device.
     */
    int getMaxPower();

    /**
     * Returns the buffer of tags read by this device.
     *
     * @return the buffer of tags read by this device.
     */
    Set<TagMetadata> getBuffer();

    /**
     * Clears the buffer of tags read by this device.
     */
    void clearBuffer();

    boolean connect(final Options opts) throws RfidDeviceException;

    void disconnect() throws RfidDeviceException;

    boolean isConnected();

    Params getInventoryParameters();

    boolean setInventoryParameters(final Params params);

    void setCallback(final Consumer<Event> callback);

    boolean startInventory() throws RfidDeviceException;

    boolean stopInventory();

    boolean isReading();

    /**
     * Destroy tag.
     *
     * @param rfid     - epc hexadecimal string.
     * @param password - tag password.
     * @return true if operation succeeded, false otherwise.
     * @throws RfidDeviceException if operation fails.
     */
    boolean killTag(final String rfid, final String password) throws RfidDeviceException;

    /**
     * Returns the frequency of the device.
     *
     * @return the frequency of the device.
     */
    Frequency getFrequency();

    /**
     * Sets the frequency of the device.
     *
     * @param frequency - the frequency to set.
     * @return true if operation succeeded, false otherwise.
     */
    boolean setFrequency(final Frequency frequency);

    /**
     * Returns the power level of the device.
     *
     * @return the power level of the device.
     */
    int getPower();

    /**
     * Sets the power level of the device.
     *
     * @param value - the power level to set.
     * @return true if operation succeeded, false otherwise.
     */
    boolean setPower(final int value);

    /**
     * Returns if device beeping is enabled.
     *
     * @return true if device beeping is enabled, false otherwise.
     */
    boolean getBeep();

    /**
     * Enables or disables device beeping.
     *
     * @param enabled - true to enable beeping, false otherwise.
     * @return true if operation succeeded, false otherwise.
     */
    boolean setBeep(final boolean enabled);

    /**
     * Enables or disables tag focus.
     *
     * @param enabled - true to enable tag focus, false otherwise.
     * @return true if operation succeeded, false otherwise.
     */
    boolean setTagFocus(final boolean enabled);

    // NESTED TYPES
    @Getter
    @RequiredArgsConstructor
    enum Frequency {

        CHINA_LOWER("China (840 ~ 845 MHz)"),
        CHINA_UPPER("China (920 ~ 925 MHz)"),
        EUROPE("Europe (865 ~ 868 MHz)"),
        UNITED_STATES("United States (902 ~ 928 MHz)"),
        KOREAN("Korea (917 ~ 923 MHz)"),
        JAPAN("Japan (916.8 ~ 920.8 MHz)"),
        SOUTH_AFRICA("South Africa (915 ~ 919 MHz)"),
        TAIWAN("Taiwan (920 ~ 928 MHz)"),
        VIETNAM("Vietnam (918 ~ 923 MHz)"),
        PERU("Peru (915 ~ 928 MHz)"),
        RUSSIA("Russia (860 ~ 867.6 MHz)"),
        MOROCCO("Morocco (914 ~ 921 MHz)"),
        MALAYSIA("Malaysia (919 ~ 923 MHz)"),
        HONG_KONG("Hong Kong (920 ~ 925 MHz)"),
        BRAZIL("Brazil (902 ~ 907.5 MHz)");

        private final String label;

    }

    @Getter
    enum Status {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        UNKNOWN
    }

    @Data
    @SuperBuilder(toBuilder = true)
    class Options {

        private final String ip;        // network ip address
        private final Integer port;     // network port

        @Builder.Default
        private final int antennas = -1;     // maximum number of antennas

        @Builder.Default
        private final boolean verbose = false;  // printout stuff

        private final String serial;    // serial port

        @Builder.Default
        private final Integer baudRate = 115_200; // serial port baud rate

        private final Short vendor;     // ???
        private final Short productId;  // ???

    }

    @Data
    class Params {

        @With
        private final String q;

        private final String x;

        private final String y;

    }

    interface Event {
    }

    @Data
    class TagEvent implements Event {

        private final TagMetadata tag;

    }

    @Data
    class StatusEvent implements Event {

        private final Status status;

    }

    @Data
    class BatteryEvent implements Event {

        private final int level;

    }

    @Data
    class ErrorEvent implements Event {

        private final Throwable cause;

    }

}
