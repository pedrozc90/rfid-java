package com.contare.rfid.objects;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Options {

    private final String serial;    // serial port
    private final String ip;        // network ip address
    private final Integer port;     // network port
    private final int antennas;     // maximum number of antennas
    private final boolean verbose;  // printout stuff

    private final Short vendor;     // ???
    private final Short productId;  // ???

    public Options(final String serial, final String ip, final Integer port, final int antennas, final boolean verbose) {
        this(serial, ip, port, antennas, verbose, (short) 0, (short) 0);
    }

}
