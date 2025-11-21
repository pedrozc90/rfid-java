package com.contare.rfid.events;

import com.contare.rfid.objects.TagMetadata;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TagEvent implements RfidDeviceEvent {

    private final TagMetadata tag;

}
