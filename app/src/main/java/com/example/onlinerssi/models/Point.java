package com.example.onlinerssi.models;

import java.util.ArrayList;

public class Point {
    private String dir;
    private ArrayList<Router> wifiList;

    public Point(String dir) {
        this.dir = dir;
        this.wifiList = new ArrayList<>();
    }

    public String getDir() {
        return dir;
    }

    public ArrayList<Router> getWifiList() {
        return wifiList;
    }

    public void setWifiList(ArrayList<Router> wifiList) {
        for (Router r : wifiList) {
            this.wifiList.add(r);
        }
    }
}
