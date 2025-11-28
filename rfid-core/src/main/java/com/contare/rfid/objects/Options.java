package com.contare.rfid.objects;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@RequiredArgsConstructor
public class Options {

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
