package com.contare.rfid.chainway;

import com.contare.rfid.exceptions.RfidDeviceException;
import com.contare.rfid.objects.Options;
import com.rscja.deviceapi.RFIDWithUHFNetworkUR4;
import com.rscja.deviceapi.entity.AntennaNameEnum;
import com.rscja.deviceapi.entity.AntennaState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ChainwayUR4 extends ChainwayDevice<RFIDWithUHFNetworkUR4> {

    private int antennas = 0;

    public ChainwayUR4(final ExecutorService executor) {
        super(new RFIDWithUHFNetworkUR4(), executor, ChainwayUR4.class);
    }

    @Override
    public boolean init(final Options opts) throws RfidDeviceException {
        final String ip = opts.getIp();
        Objects.requireNonNull(ip, "IP address is required.");

        final Integer port = opts.getPort();
        Objects.requireNonNull(port, "Port is required.");

        this.antennas = opts.getAntennas();

        boolean connected = uhf.init(ip, port);
        if (connected) {
            logger.debugf("Device successfully connected to %s:%d", ip, port);
        } else {
            logger.errorf("Device failed to connect to %s:%d", ip, port);
        }
        return connected;
    }

    @Override
    public int getPower() {
        final List<Integer> results = new ArrayList<>();
        for (int ant = 1; ant <= antennas; ant++) {
            final AntennaNameEnum antenna = AntennaNameEnum.getValue(ant);
            if (antenna != null) {
                int power = uhf.getPower(antenna);
                results.add(power);
            }
        }
        int total = results.stream().reduce(Integer::sum).orElse(0);
        int count = (int) results.size();
        return (int) (total / count);
    }

    @Override
    public boolean setPower(final int value) {
        if (value < _minPower || value > _maxPower) {
            throw new IllegalArgumentException(String.format("'power' must be between '%d' amd '%d'", _minPower, _maxPower));
        }

        for (int ant = 1; ant <= antennas; ant++) {
            final AntennaNameEnum antenna = AntennaNameEnum.getValue(ant);
            if (antenna != null) {
                boolean updated = uhf.setPower(antenna, value);
                if (!updated) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Set the antennas enable state.
     *
     * @param array - array os 0/1 values, where the index corresponds to the antenna number (index + 1 = antenna position).
     * @return true if operation succeeded, false otherwise.
     */
    public boolean setAntennas(final int[] array) {
        final List<AntennaState> value = new ArrayList<>();
        for (int index = 0; index < array.length; index++) {
            final AntennaNameEnum ant = AntennaNameEnum.getValue(index + 1);
            final boolean enabled = array[index] == 1;
            final AntennaState state = new AntennaState(ant, enabled);
            value.add(state);
        }
        return uhf.setAntenna(value);
    }

    /**
     * Restore factory settings.
     *
     * @return true if operation succeeded, false otherwise.
     */
    public boolean reset() {
        return uhf.resetUHFSoft();
    }

}
