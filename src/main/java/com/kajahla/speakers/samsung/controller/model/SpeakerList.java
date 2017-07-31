package com.kajahla.speakers.samsung.controller.model;

public class SpeakerList {
    private String[] speakerName = new String[5];

    public SpeakerList() {

    }

    public SpeakerList(String[] speakerName) {
        this.speakerName = speakerName;
    }


    public String[] getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String[] speakerName) {
        this.speakerName = speakerName;
    }

    public int getLength() {
        return this.speakerName.length;
    }

    public String getSpeaker(int index) {
        return this.speakerName[index];
    }
}
