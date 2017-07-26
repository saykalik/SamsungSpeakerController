package com.kajahla.speakers.samsung.controller;

import com.kajahla.speakers.samsung.controller.model.GroupInfo;
import com.kajahla.speakers.samsung.controller.model.SpeakerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.USER_AGENT;

@RestController
public class SpeakerController {

    private HashMap<String, SpeakerInfo> speakers = new HashMap<>();
    private HashMap<String, GroupInfo> speakerGroups = new HashMap<>();


    public SpeakerController() {
        SpeakerInfo kitchen = new SpeakerInfo("kitchen", "192.168.10.197", "55001","f8:77:b8:c4:8f:a1");
        SpeakerInfo livingroom = new SpeakerInfo("livingroom", "192.168.10.238", "55001","f8:77:b8:c4:8f:ca");
        SpeakerInfo boysroom = new SpeakerInfo("boysroom", "192.168.10.200", "55001","f8:77:b8:c5:55:a0");
        SpeakerInfo bathroom = new SpeakerInfo("bathroom", "192.168.10.110", "55001","f8:77:b8:c5:d5:78");

        speakers.put(kitchen.getName(), kitchen);
        speakers.put(livingroom.getName(), livingroom);
        speakers.put(boysroom.getName(), boysroom);
        speakers.put(bathroom.getName(), bathroom);
    }

    @RequestMapping("/group")
    public ResponseEntity Group(@RequestParam(value="group_name", required = false) String groupName,
                                @RequestParam(value="speaker") String[] speakerName) {

        // Check if we already have a group
        if (speakerGroups.size() > 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is already a group");

        // Check to make sure the group doesn't already exist
        GroupInfo groupInfo = null;
        if (groupName != null && groupName.length() > 0) {
            groupInfo = speakerGroups.get(groupName);
            if (groupInfo != null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group already exists");
        }

        // Check to make sure speaker name list is not empty
        if (speakerName.length == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        // Create a group and send the command to the speakers to join
        groupInfo = new GroupInfo();
        if (groupName != null && groupName.length() > 0)
            groupInfo.setName(groupName);
        else
            groupInfo.generateName();

        for (int ix = 0; ix < speakerName.length; ix++)
            groupInfo.addSpeaker(speakers.get(speakerName[ix]));

        SpeakerInfo masterSpeaker = groupInfo.getSpeaker(groupInfo.getMasterSpeakerName());
        String masterUrl = "http://" + masterSpeaker.getIp() + ":" + masterSpeaker.getPort() +
                "/UIC?cmd=<pwron>on</pwron><name>SetMultispkGroup</name>" +
                "<p type=\"cdata\" name=\"name\" val=\"empty\"><![CDATA[" + groupInfo.getName() + "]]></p>" +
                "<p type=\"dec\" name=\"index\" val=\"1\"/>" +
                "<p type=\"str\" name=\"type\" val=\"main\"/>" +
                "<p type=\"dec\" name=\"spknum\" val=\"" + groupInfo.getNumSpeakers() + "\"/>" +
                "<p type=\"str\" name=\"audiosourcemacaddr\" val=\"" + masterSpeaker.getMac() + "\"/>" +
                "<p type=\"cdata\" name=\"audiosourcename\" val=\"empty\"><![CDATA[" + masterSpeaker.getName() + "]]></p>" +
                "<p type=\"str\" name=\"audiosourcetype\" val=\"speaker\"/>";

        for (Map.Entry<String, SpeakerInfo> entry : groupInfo.getSpeakerMap().entrySet()) {
            String key = entry.getKey();
            SpeakerInfo slaveSpeaker = entry.getValue();

            if (slaveSpeaker.getName() == masterSpeaker.getName())
                continue;

            masterUrl += "<p type=\"str\" name=\"subspkip\" val=\"" + slaveSpeaker.getIp() + "\"/>" +
                    "<p type=\"str\" name=\"subspkmacaddr\" val=\"" + slaveSpeaker.getMac() + "\"/>";
        }

        // Generate all slave urls
        ArrayList<String> slaveUrls = new ArrayList<>();
        for (Map.Entry<String, SpeakerInfo> entry : groupInfo.getSpeakerMap().entrySet()) {
            String key = entry.getKey();
            SpeakerInfo slaveSpeaker = entry.getValue();

            if (slaveSpeaker.getName() == masterSpeaker.getName())
                continue;

            String slaveUrl = "http://" + slaveSpeaker.getIp() + ":" + slaveSpeaker.getPort() +
                    "/UIC?cmd=<pwron>on</pwron><name>SetMultispkGroup</name>" +
                    "<p type=\"cdata\" name=\"name\" val=\"empty\"><![CDATA[" + groupInfo.getName() + "]]></p>" +
                    "<p type=\"dec\" name=\"index\" val=\"1\"/>" +
                    "<p type=\"str\" name=\"type\" val=\"sub\"/>" +
                    "<p type=\"dec\" name=\"spknum\" val=\"" + groupInfo.getNumSpeakers() + "\"/>" +
                    "<p type=\"str\" name=\"mainspkip\" val=\"" + masterSpeaker.getIp() + "\"/>" +
                    "<p type=\"str\" name=\"mainspkmacaddr\" val=\"" + masterSpeaker.getMac() + "\"/>";

            slaveUrls.add(slaveUrl);
        }


        try {

            sendGet(masterUrl);
            for (String slaveUrl : slaveUrls)
                sendGet(slaveUrl);

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ResponseEntity.ok("ok");
    }

    @RequestMapping("/ungroup")
    public ResponseEntity UnGroup(@RequestParam(value="group_name", required = false) String groupName) {

        // Check to make sure the group exists.
        GroupInfo groupInfo = null;
        if (groupName != null && groupName.length() > 0) {
            groupInfo = speakerGroups.get(groupName);
            if (groupInfo == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            // If the group exists then we get the master speaker and send an ungroup command to it
            SpeakerInfo masterSpeaker = groupInfo.getSpeaker(groupInfo.getMasterSpeakerName());
            String url = "http://" + masterSpeaker.getIp() + ":" + masterSpeaker.getPort() + "/UIC?cmd=<name>SetUngroup</name>";
            try {
                sendGet(url);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // There are no groups so go through all speakers and ungroup them
        else {

            for (Map.Entry<String, SpeakerInfo> entry : speakers.entrySet()) {
                SpeakerInfo speaker = entry.getValue();

                String url = "http://" + speaker.getIp() + ":" + speaker.getPort() + "/UIC?cmd=<name>SetUngroup</name>";
                try {
                    sendGet(url);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return ResponseEntity.ok("ok");
    }

    // HTTP GET request
    private void sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        System.out.println("\nSending 'GET' request to URL : " + url);

        int responseCode = con.getResponseCode();

        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
    }


}
