package com.kajahla.speakers.samsung.controller;

import com.kajahla.speakers.samsung.controller.model.GroupInfo;
import com.kajahla.speakers.samsung.controller.model.SpeakerInfo;
import com.kajahla.speakers.samsung.controller.model.SpeakerList;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import java.io.*;
import java.util.Map;

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
            jmdns.addServiceListener("_spotify-connect._tcp.local.", new SamsungSpeakerListener());

            // Wait a bit
            Thread.sleep(30000);

        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private class SamsungSpeakerListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) { }

        @Override
        public void serviceResolved(ServiceEvent event) {
            if (event.getInfo().getInetAddresses() != null && event.getInfo().getInetAddresses().length > 0) {
                String url = "http://" + event.getInfo().getInetAddresses()[0].getHostAddress() +":55001/UIC?cmd=<name>GetApInfo</name>";

                String response;
                try {
                    response = sendGet(url);

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(new InputSource(new ByteArrayInputStream(response.getBytes("utf-8"))));

                    NodeList list = document.getElementsByTagName("mac");

                    if (list.getLength() > 0) {
                        Element node = (Element) list.item(0);

                        SpeakerInfo newSpeaker = new SpeakerInfo(event.getName().toLowerCase(),
                                event.getInfo().getInetAddresses()[0].getHostAddress(),
                                "55001",
                                 node.getFirstChild().getNodeValue());
                        speakers.put(newSpeaker.getName().toLowerCase(), newSpeaker);

                        System.out.println("Found and Added Speaker -> " + newSpeaker.toString());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @RequestMapping(path = "/group", consumes = "application/json", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity Group(@RequestParam(value="group_name", required = false) String groupName,
                                @RequestBody SpeakerList speakerName) {

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
        if (speakerName.getLength() == 0)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        // Create a group and send the command to the speakers to join
        groupInfo = new GroupInfo();
        if (groupName != null && groupName.length() > 0)
            groupInfo.setName(groupName);
        else
            groupInfo.generateName();

        for (int ix = 0; ix < speakerName.getLength(); ix++) {
            if (speakers.get(speakerName.getSpeaker(ix)) == null)
                continue;
            groupInfo.addSpeaker(speakers.get(speakerName.getSpeaker(ix).toLowerCase()));
        }

        if (groupInfo.getNumSpeakers() < 2)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

        SpeakerInfo masterSpeaker = groupInfo.getSpeaker(groupInfo.getMasterSpeakerName().toLowerCase());
        String masterUrl = "http://" + masterSpeaker.getIp() + ":" + masterSpeaker.getPort() + "/UIC?cmd=";
        String command = "<pwron>on</pwron><name>SetMultispkGroup</name>" +
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

            if (slaveSpeaker.getName().toLowerCase() == groupInfo.getMasterSpeakerName().toLowerCase())
                continue;

            command += "<p type=\"str\" name=\"subspkip\" val=\"" + slaveSpeaker.getIp() + "\"/>" +
                    "<p type=\"str\" name=\"subspkmacaddr\" val=\"" + slaveSpeaker.getMac() + "\"/>";
        }

        try {
            sendGet(masterUrl, command);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ResponseEntity.ok("{ \"status\": \"success\", \"group_name\": \"" + groupInfo.getName() + "\" }");
    }

    @RequestMapping("/ungroup")
    public ResponseEntity UnGroup(@RequestParam(value="group_name", required = false) String groupName) {

        // Check to make sure the group exists.
        GroupInfo groupInfo = null;
        if (groupName != null && groupName.length() > 0) {
            groupInfo = speakerGroups.get(groupName);
            if (groupInfo == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group does not exist");


            // If the group exists then we get the master speaker and send an ungroup command to it
            SpeakerInfo masterSpeaker = groupInfo.getSpeaker(groupInfo.getMasterSpeakerName().toLowerCase());
            String url = "http://" + masterSpeaker.getIp() + ":" + masterSpeaker.getPort() + "/UIC?cmd=<name>SetUngroup</name>";

            try {
                sendGet(url);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        else {
            // Send ungroup to every speaker we know
            for (Map.Entry<String, SpeakerInfo> entry : speakers.entrySet()) {
                String key = entry.getKey();
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

    private String sendGet(String url) throws Exception {
        return sendGet(url, "", false);
    }


    private String sendGet(String url, String command) throws Exception {
        return sendGet(url, command,false);
    }


    // HTTP GET request
    private String sendGet(String url, String command, boolean print) throws Exception {

        String requestString = url;
        if (command.length() > 0) {
            requestString += percentEncode(command);
        }
        URL obj = new URL(requestString);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setUseCaches(false);
        con.setRequestProperty("Accept", "*/*");
        con.setRequestProperty("Accept-Language", "en-GB,en-US;q=0.8,en;q=0.6");

        int responseCode = con.getResponseCode();
        if (print) {
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
        }

        StringBuffer response = new StringBuffer();

        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //print result
        if (print)
            System.out.println(response.toString());

        return response.toString();
    }


    // For some reason the URLEncode call didn't encode the URL the way the
    // speakers expected so the call would either fail or hang.  Used Postman
    // and wireshark to find what worked and created this to replicate what
    // postman was doing.
    public static String percentEncode(String encodeMe) {
        if (encodeMe == null) {
            return "";
        }
        String encoded = encodeMe.replace("%", "%25");
        encoded = encoded.replace(" ", "%20");
        //encoded = encoded.replace("!", "%21");
        encoded = encoded.replace("\"", "%22");
        encoded = encoded.replace("#", "%23");
        encoded = encoded.replace("$", "%24");
        encoded = encoded.replace("&", "%26");
        encoded = encoded.replace("'", "%27");
        encoded = encoded.replace("(", "%28");
        encoded = encoded.replace(")", "%29");
        encoded = encoded.replace("*", "%2A");
        //encoded = encoded.replace("+", "%2B");
        encoded = encoded.replace(",", "%2C");
        //encoded = encoded.replace("/", "%2F");
        encoded = encoded.replace(":", "%3A");
        encoded = encoded.replace(";", "%3B");
        encoded = encoded.replace("<", "%3C");
        //encoded = encoded.replace("=", "%3D");
        encoded = encoded.replace(">", "%3E");
        encoded = encoded.replace("?", "%3F");
        encoded = encoded.replace("@", "%40");
        //encoded = encoded.replace("[", "%5B");
        //encoded = encoded.replace("]", "%5D");
        return encoded;
    }
}
