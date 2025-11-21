package com.contare.rfid.objects;

import lombok.Getter;

@Getter
public enum Status {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    UNKNOWN
}
