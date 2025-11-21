package com.contare.rfid.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;

@Data
@AllArgsConstructor
public class RfidDeviceParams {

    @With
    private final String q;

    private final String x;

    private final String y;

}
