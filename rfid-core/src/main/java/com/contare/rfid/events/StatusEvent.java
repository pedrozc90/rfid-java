package com.contare.rfid.events;

import com.contare.rfid.objects.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatusEvent implements RfidDeviceEvent {

    private final Status status;

}
