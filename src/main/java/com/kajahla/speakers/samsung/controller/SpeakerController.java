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
import java.util.HashMap;

import static org.springframework.http.HttpHeaders.USER_AGENT;

@RestController
public class SpeakerController {

    private HashMap<String, SpeakerInfo> speakers = new HashMap<>();
    private HashMap<String, HashMap<String, GroupInfo>> speakerGroups = new HashMap<>();


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
    public ResponseEntity Group(@RequestParam(value="speaker") String[] speakerName) {

        if (speakerName.length == 0)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        try {
            sendGet(" http://192.168.10.197:55001/UIC?cmd=<name>SetUngroup</name>");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ResponseEntity.ok("ok");
    }

    @RequestMapping("/ungroup")
    public ResponseEntity UnGroup(@RequestParam(value="group_name", required = false) String groupName) {

        HashMap<String, GroupInfo> groupInfo = null;
        if (groupName.length() > 0) {
            groupInfo = speakerGroups.get(groupName);
            if (groupInfo == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

            try {
                sendGet(" http://192.168.10.197:55001/UIC?cmd=<name>SetUngroup</name>");
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
