package com.kajahla.speakers.samsung.controller;

import com.kajahla.speakers.samsung.controller.model.GroupInfo;
import com.kajahla.speakers.samsung.controller.model.SpeakerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;

import static org.springframework.http.HttpHeaders.USER_AGENT;

@RestController
public class SpeakerController {

    private HashMap<String, SpeakerInfo> speakers = new HashMap<>();
    private HashMap<String, GroupInfo> speakerGroups = new HashMap<>();
    private JmDNS jmdns = null;

    public SpeakerController() {
        try {
            // Create a JmDNS instance
            jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Add a service listener
            jmdns.addServiceListener("_spotify-connect._tcp.local.", new SampleListener());

            // Wait a bit
            Thread.sleep(30000);

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }


        SpeakerInfo kitchen = new SpeakerInfo("kitchen", "192.168.10.197", "55001","f8:77:b8:c4:8f:a1");
        SpeakerInfo livingroom = new SpeakerInfo("livingroom", "192.168.10.238", "55001","f8:77:b8:c4:8f:ca");
        SpeakerInfo boysroom = new SpeakerInfo("boysroom", "192.168.10.200", "55001","f8:77:b8:c5:55:a0");
        SpeakerInfo bathroom = new SpeakerInfo("bathroom", "192.168.10.110", "55001","f8:77:b8:c5:d5:78");

        speakers.put(kitchen.getName(), kitchen);
        speakers.put(livingroom.getName(), livingroom);
        speakers.put(boysroom.getName(), boysroom);
        speakers.put(bathroom.getName(), bathroom);
    }

    private class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
//            System.out.println("Service added: " + event.getInfo());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
//            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
//            System.out.println("Service resolved: " + event.getInfo());

            if (event.getInfo().getInetAddresses() != null && event.getInfo().getInetAddresses().length > 0) {
                System.out.println("Service name: " + event.getName());
                System.out.println("Service ip: " + event.getInfo().getInetAddresses()[0].getHostAddress());
            }
        }
    }


    @RequestMapping("/group")
    public ResponseEntity Group(@RequestParam(value="group_name", required = false) String groupName,
                                @RequestParam(value="speaker") String[] speakerName) {

        // Check if we already have a group
        if (speakerGroups.size() > 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is already a group");

        // Check to make sure the group doesn't already exist
        GroupInfo groupInfo = null;
        if (groupName.length() > 0) {
            groupInfo = speakerGroups.get(groupName);
            if (groupInfo != null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group already exists");
        }

        // Check to make sure speaker name list is not empty
        if (speakerName.length == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        // Create a group and send the command to the speakers to join
        groupInfo = new GroupInfo();
        if (groupName.length() > 0)
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

        Iterator it = groupInfo.getSpeakerMap().entrySet().iterator();
        while (it.hasNext()) {
            SpeakerInfo slaveSpeaker = (SpeakerInfo)it.next();
            masterUrl += "<p type=\"str\" name=\"subspkip\" val=\"" + slaveSpeaker.getIp() + "\"/>" +
                    "<p type=\"str\" name=\"subspkmacaddr\" val=\"" + slaveSpeaker.getMac() + "\"/>";
        }

        try {
            sendGet(masterUrl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ResponseEntity.ok("ok");
    }

    @RequestMapping("/ungroup")
    public ResponseEntity UnGroup(@RequestParam(value="group_name", required = false) String groupName) {

        // Check to make sure the group exists.
        GroupInfo groupInfo = null;
        if (groupName.length() > 0) {
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

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
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
