package com.contare.rfid.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TagMetadata {

    public final String rfid;

    public final String tid;

    public final String rssi;

    public final Integer antenna;

    public final Instant timestamp = Instant.now();

}
