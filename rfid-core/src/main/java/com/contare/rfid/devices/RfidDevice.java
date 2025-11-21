package com.contare.rfid.devices;

import com.contare.rfid.events.RfidDeviceEvent;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.Options;
import com.contare.rfid.objects.RfidDeviceFrequency;
import com.contare.rfid.objects.RfidDeviceParams;
import com.contare.rfid.objects.TagMetadata;

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

    void disconnect();

    boolean isConnected();

    RfidDeviceParams getInventoryParameters();

    boolean setInventoryParameters(final RfidDeviceParams params);

    void setCallback(final Consumer<RfidDeviceEvent> callback);

    boolean startInventory() throws RfidDeviceException;

    boolean stopInventory();

    boolean isReading();

    /**
     * Returns the frequency of the device.
     *
     * @return the frequency of the device.
     */
    RfidDeviceFrequency getFrequency();

    /**
     * Sets the frequency of the device.
     *
     * @param frequency - the frequency to set.
     * @return true if operation succeeded, false otherwise.
     */
    boolean setFrequency(final RfidDeviceFrequency frequency);

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

}
