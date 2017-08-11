# SamsungSpeakerController
I wanted the ability to create speaker groups without opening up the app but by using Alexa. I want to be able to say "Alexa ask Samsung to create groupone with the kitchen and livingroom speakers" then have an Alexa intent do it for me. I then want to tell Alexa to use this group "Alexa ask Spotify to play my playlist on groupone". I don't have a smartthings hub and not sure if it can be done with that but since I don't have it I am going to use Alexa and Home Assitant. To be able to do that I needed to figure out how the HTTP Group calls to the speakers worked. Here is my findings:

The way it works, as far as I can see, is that when you create a group you assign one of the speakers as the master (from the code it looks like it picks the one that was first selected to be part of the group)
You sends a specific call to a speaker to let it know it's the master, the name of the group, and the slave speakers IP addresses and mac addresses.
It then sends a specific call to each slave speaker that is part of the group and provide the masters IP address and mac address.
Here are the calls that go out to my speakers. I am using two speakers. The master is the "Kitchen" speaker with an IP address of 1.2.3.4 and a MAC address of 01:02:03:04:05:06. The slave is the "Boysroom" speaker with an IP address of 9.8.7.6 and a MAC address of 11:22:33:44:55:66.

First, call the master speaker with the all the info about the group:
http://1.2.3.4:55001/UIC?cmd=<pwron>on</pwron><name>SetMultispkGroup</name>
<p type="cdata" name="name" val="empty"><![CDATA[Kitchen + Boysroom]]></p>
<p type="dec" name="index" val="1"/>
<p type="str" name="type" val="main"/>
<p type="dec" name="spknum" val="2"/>
<p type="str" name="audiosourcemacaddr" val="01:02:03:04:05:06"/>
<p type="cdata" name="audiosourcename" val="empty"><![CDATA[Kitchen]]></p>
<p type="str" name="audiosourcetype" val="speaker"/>
<p type="str" name="subspkip" val="9.8.7.6"/>
<p type="str" name="subspkmacaddr" val="11:22:33:44:55:66"/>

Then call the slave speaker
http://9.8.7.6:55001/UIC?cmd=<pwron>on</pwron><name>SetMultispkGroup</name>
<p type="cdata" name="name" val="empty"><![CDATA[Kitchen + Boysroom]]></p>
<p type="dec" name="index" val="1"/>
<p type="str" name="type" val="sub"/>
<p type="dec" name="spknum" val="2"/>
<p type="str" name="mainspkip" val="1.2.3.4"/>
<p type="str" name="mainspkmacaddr" val="01:02:03:04:05:06"/>

Keep in mind for the master call:
1. Name can be anything string you want and is set in the CDATA
2. I found index 1 always works (not sure what its for though)
3. Type is "main"
4. spknum is the number of speakers in the group including the master

Keep in mind for the slave call:
1. Name matches what was sent to the master
2. Index is 1, same as master
3. Type is "sub"
4. spknum is the number of speakers in the group including the master. Same as master.

I hope this helps someone else.
