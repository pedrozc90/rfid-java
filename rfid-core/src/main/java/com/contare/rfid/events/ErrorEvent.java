package com.contare.rfid.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorEvent implements RfidDeviceEvent {

    private final Throwable cause;

}
