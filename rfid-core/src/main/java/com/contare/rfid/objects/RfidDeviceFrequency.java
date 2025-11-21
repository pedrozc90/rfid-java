package com.contare.rfid.objects;

import lombok.Getter;

@Getter
public enum RfidDeviceFrequency {

    CHINA_LOWER("China (840 ~ 845 MHz)"),
    CHINA_UPPER("China (920 ~ 925 MHz)"),
    EUROPE("Europe (865 ~ 868 MHz)"),
    UNITED_STATES("United States (902 ~ 928 MHz)"),
    KOREAN("Korea (917 ~ 923 MHz)"),
    JAPAN("Japan (916.8 ~ 920.8 MHz)"),
    SOUTH_AFRICA("South Africa (915 ~ 919 MHz)"),
    TAIWAN("Taiwan (920 ~ 928 MHz)"),
    VIETNAM("Vietnam (918 ~ 923 MHz)"),
    PERU("Peru (915 ~ 928 MHz)"),
    RUSSIA("Russia (860 ~ 867.6 MHz)"),
    MOROCCO("Morocco (914 ~ 921 MHz)"),
    MALAYSIA("Malaysia (919 ~ 923 MHz)"),
    BRAZIL("Brazil (902 ~ 907.5 MHz)");

    private final String label;

    RfidDeviceFrequency(final String label) {
        this.label = label;
    }

}
