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

    public SpeakerInfo getSpeaker(String name) {
        return speakers.get(name);
    }

    public void addSpeaker (SpeakerInfo speaker) {

        // If this is the first speaker to be added to the group it becomes the master speaker
        if (speakers.size() == 0)
            masterSpeakerName = speaker.getName();

        speakers.put(speaker.getName(), speaker);
    }
}
