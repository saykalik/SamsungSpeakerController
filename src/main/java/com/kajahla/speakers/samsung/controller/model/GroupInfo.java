package com.kajahla.speakers.samsung.controller.model;

import java.util.HashMap;
import java.util.Iterator;

public class GroupInfo {
    private String name = new String();
    private String masterSpeakerName = new String();
    HashMap<String, SpeakerInfo> speakers = new HashMap<>();

    public void generateName() {
        name = "";

        Iterator it = speakers.entrySet().iterator();
        while (it.hasNext()) {
            SpeakerInfo speaker = (SpeakerInfo)it.next();
            name += speaker.getName() + " + ";
        }
    }

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

    public int getNumSpeakers() {
        return speakers.size();
    }

    public HashMap<String, SpeakerInfo> getSpeakerMap() {
        return speakers;
    }
}
