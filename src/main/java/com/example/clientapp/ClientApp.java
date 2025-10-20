package com.example.clientapp;

import com.example.clientapp.config.PropertiesInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class ClientApp implements CommandLineRunner {

    private WebClient webClient;
    private final Map<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();
    private final Duration apiCooldown = Duration.ofMinutes(5);

    private final WebClient.Builder webClientBuilder;
    private final PropertiesInfo propertiesInfo;

    @Autowired
    public ClientApp(WebClient.Builder webClientBuilder, PropertiesInfo propertiesInfo) {
        this.webClientBuilder = webClientBuilder;
        this.propertiesInfo = propertiesInfo;
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl("http://" + propertiesInfo.getMainServerIp() + ":" + propertiesInfo.getServerPort())
                .build();

        System.out.println("✅ WebClient initialized with: " +
                "http://" + propertiesInfo.getMainServerIp() + ":" + propertiesInfo.getServerPort());
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("Listening for Windows login/logout events via Event Log...");

        String lastEventId = "";

        while (true) {
            try {
                Event latestEvent = getLatestEvent();
                if (latestEvent != null && !latestEvent.recordId.equals(lastEventId)) {

                    String username = latestEvent.username;
                    String eventType = latestEvent.eventType;

                    if ("LOGIN".equals(eventType)) {
                        LocalDateTime lastHit = activeUsers.get(username);
                        if (lastHit == null || Duration.between(lastHit, LocalDateTime.now()).compareTo(apiCooldown) > 0) {
                            sendEvent(username, eventType);
                            activeUsers.put(username, LocalDateTime.now());
                        } else {
                            System.out.println("Skipping API hit for " + username + " (still within 5 mins)");
                        }
                    } else if ("LOGOUT".equals(eventType)) {
                        sendEvent(username, eventType);
                        activeUsers.remove(username); // reset so next login triggers API
                    }

                    lastEventId = latestEvent.recordId;
                }

                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Event getLatestEvent() {
        try {
            ProcessBuilder pb = new ProcessBuilder("wevtutil", "qe", "Security", "/q:*[System[(EventID=4624 or EventID=4634)]]", "/f:xml", "/c:1");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xml.append(line);
            }
            if (xml.isEmpty()) return null;
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.toString().getBytes()));
            String recordId = doc.getElementsByTagName("EventRecordID").item(0).getTextContent();
            String eventId = doc.getElementsByTagName("EventID").item(0).getTextContent();
            NodeList dataList = doc.getElementsByTagName("Data");
            String username = "";
            for (int i = 0; i < dataList.getLength(); i++) {
                String name = dataList.item(i).getAttributes().getNamedItem("Name").getTextContent();
                if ("TargetUserName".equalsIgnoreCase(name)) {
                    username = dataList.item(i).getTextContent();
                    break;
                }
            }
            String eventType = "UNKNOWN";
            if ("4624".equals(eventId)) eventType = "LOGIN";
            if ("4634".equals(eventId)) eventType = "LOGOUT";
            return new Event(recordId, username, eventType);
        } catch (Exception e) {
             e.printStackTrace();
            return null;
        }
    }

    private void sendEvent(String username, String eventType) {
        String timestamp = java.time.LocalDateTime.now().toString();
        String systemIp = getSystemIp();
        UserEvent event = new UserEvent(username, eventType, timestamp, systemIp);
        System.out.println(" User Event : "+event);
        Boolean response = webClient.post().uri("/api/user-event").bodyValue(event).retrieve().bodyToMono(Boolean.class).block();
        System.out.println("API response: " + response);
    }

    private String getSystemIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

    record Event(String recordId, String username, String eventType) {
    }

    record UserEvent(String username, String eventType, String timestamp, String systemIp) {
    }
}
