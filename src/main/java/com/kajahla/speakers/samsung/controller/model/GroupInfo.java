package com.kajahla.speakers.samsung.controller.model;

import java.util.HashMap;

public class GroupInfo {
    private String name;
    private String masterSpeakerName;
    HashMap<String, SpeakerInfo> speakers = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMasterSpeakerName() {
        return masterSpeakerName;
    }

    public void setMasterSpeakerName(String masterSpeaker) {
        this.masterSpeakerName = masterSpeaker;
    }

    public SpeakerInfo getSpeaker(String name) {
        return speakers.get(name);
    }

    public void AddSpeaker (SpeakerInfo speaker) {
        speakers.put(speaker.getName(), speaker);
    }
}
