package com.contare.rfid.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BatteryEvent implements RfidDeviceEvent {

    private final int level;

}
