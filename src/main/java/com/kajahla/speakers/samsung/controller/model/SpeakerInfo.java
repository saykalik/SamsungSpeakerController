package com.kajahla.speakers.samsung.controller.model;

public class SpeakerInfo {
    private String name;
    private String mac;
    private String ip;
    private String port;
    private String groupName;
    private boolean isMaster = false;

    public SpeakerInfo(String name, String ip, String port, String mac) {
        this.name = name;
        this.ip = ip;
        this.mac = mac;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }
}
