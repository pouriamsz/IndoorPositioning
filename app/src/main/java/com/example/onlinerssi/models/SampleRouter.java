package com.example.onlinerssi.models;

import java.util.ArrayList;

public class SampleRouter {
    private String SSID, BSSID;
    private Integer RSSI;

    public SampleRouter(String SSID, String BSSID) {
        this.SSID = SSID;
        this.BSSID = BSSID;
    }

    public void setRSSI(Integer RSSI) {
        this.RSSI = RSSI;
    }

    public String getSSID() {
        return SSID;
    }

    public String getBSSID() {
        return BSSID;
    }

    public Integer getRSSI() {
        return RSSI;
    }
}
