package com.kajahla.speakers.samsung.controller.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GroupInfo {
    private String name = new String();
    private String masterSpeakerName = new String();
    HashMap<String, SpeakerInfo> speakers = new HashMap<>();

    public void generateName() {
        name = "";

        if (masterSpeakerName.length() > 0)
            name = masterSpeakerName;

        for (Map.Entry<String, SpeakerInfo> entry : speakers.entrySet()) {
            String key = entry.getKey();
            SpeakerInfo speaker = entry.getValue();

            if (speaker.getName() == masterSpeakerName)
                continue;

            name += " + " + speaker.getName();
        }
    }

    public String getName() {
        if (name == null || name.length() == 0)
            generateName();

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

    public int getNumSpeakers() {
        return speakers.size();
    }

    public HashMap<String, SpeakerInfo> getSpeakerMap() {
        return speakers;
    }
}
