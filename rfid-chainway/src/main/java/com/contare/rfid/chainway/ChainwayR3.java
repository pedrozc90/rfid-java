package com.contare.rfid.chainway;

import com.contare.rfid.devices.RfidDevice;
import com.contare.rfid.exceptions.RfidDeviceException;
import com.rscja.deviceapi.RFIDWithUHFUsb;
import com.rscja.deviceapi.interfaces.KeyEventCallback;

import java.util.concurrent.ExecutorService;

public class ChainwayR3 extends ChainwayDevice<RFIDWithUHFUsb> {

    public ChainwayR3(final ExecutorService executor) {
        super(RFIDWithUHFUsb.getInstance(), executor, ChainwayR3.class);
    }

    @Override
    public boolean init(final RfidDevice.Options opts) throws RfidDeviceException {
        boolean connected = uhf.init(null);
        if (connected) {
            logger.debugf("Device successfully connected");
        } else {
            logger.errorf("Device failed to connect");
        }

        uhf.setKeyEventCallback(new KeyEventCallback() {
            @Override
            public void onKeyDown(int i) {
                logger.debugf("Key '%d' down", i);
            }

            @Override
            public void onKeyUp(int i) {
                logger.debugf("Key '%d' up", i);
            }
        });

        return connected;
    }

    @Override
    public int getPower() {
        return uhf.getPower();
    }

    @Override
    public boolean setPower(final int value) {
        if (value < _minPower || value > _maxPower) {
            throw new IllegalArgumentException(String.format("'power' must be between '%d' amd '%d'", _minPower, _maxPower));
        }
        return uhf.setPower(value);
    }

}
