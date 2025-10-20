package com.example.clientapp;

import com.example.clientapp.config.PropertiesInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ClientApp.class);

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
        String baseUrl = "http://" + propertiesInfo.getMainServerIp() + ":" + propertiesInfo.getServerPort();
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();

        log.info("✅ WebClient initialized with base URL: {}", baseUrl);
        log.info("Loaded properties: mainServerIp={}, port={}",
                propertiesInfo.getMainServerIp(), propertiesInfo.getServerPort());
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("🔍 Starting Windows event listener for login/logout events...");

        String lastEventId = "";

        while (true) {
            try {
                Event latestEvent = getLatestEvent();

                if (latestEvent != null && !latestEvent.recordId.equals(lastEventId)) {
                    String username = latestEvent.username;
                    String eventType = latestEvent.eventType;

                    log.info("🆕 New event detected: [RecordID={}, User={}, Type={}]", latestEvent.recordId, username, eventType);

                    if ("LOGIN".equals(eventType)) {
                        LocalDateTime lastHit = activeUsers.get(username);
                        if (lastHit == null || Duration.between(lastHit, LocalDateTime.now()).compareTo(apiCooldown) > 0) {
                            log.info("🚀 Sending LOGIN event for user '{}'", username);
                            sendEvent(username, eventType);
                            activeUsers.put(username, LocalDateTime.now());
                        } else {
                            log.debug("Skipping API hit for '{}' (still within cooldown: {} mins)",
                                    username, apiCooldown.toMinutes());
                        }
                    } else if ("LOGOUT".equals(eventType)) {
                        log.info("📴 Sending LOGOUT event for user '{}'", username);
                        sendEvent(username, eventType);
                        activeUsers.remove(username);
                    } else {
                        log.warn("⚠️ Unknown event type detected: {}", eventType);
                    }

                    lastEventId = latestEvent.recordId;
                }

                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("❌ Exception in main event loop: {}", e.getMessage(), e);
            }
        }
    }

    private Event getLatestEvent() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "wevtutil", "qe", "Security",
                    "/q:*[System[(EventID=4624 or EventID=4634)]]", "/f:xml", "/c:1"
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xml.append(line);
            }

            if (xml.isEmpty()) {
                log.debug("No new event detected in Windows Security log.");
                return null;
            }

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(xml.toString().getBytes()));

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

            String eventType = switch (eventId) {
                case "4624" -> "LOGIN";
                case "4634" -> "LOGOUT";
                default -> "UNKNOWN";
            };

            log.debug("Parsed Event: recordId={}, username={}, eventId={}, eventType={}",
                    recordId, username, eventId, eventType);

            return new Event(recordId, username, eventType);

        } catch (Exception e) {
            log.error("Error while reading Windows Event Log: {}", e.getMessage(), e);
            return null;
        }
    }

    private void sendEvent(String username, String eventType) {
        try {
            String timestamp = LocalDateTime.now().toString();
            String systemIp = getSystemIp();
            UserEvent event = new UserEvent(username, eventType, timestamp, systemIp);

            log.info("📡 Sending event to server: {}", event);

            Boolean response = webClient.post()
                    .uri("/api/user-event")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            log.info("✅ API response for user '{}': {}", username, response);

        } catch (Exception e) {
            log.error("❌ Failed to send event for user '{}': {}", username, e.getMessage(), e);
        }
    }

    private String getSystemIp() {
        try {
            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            log.debug("Resolved system IP: {}", ip);
            return ip;
        } catch (Exception e) {
            log.error("Failed to resolve system IP: {}", e.getMessage(), e);
            return "UNKNOWN";
        }
    }

    record Event(String recordId, String username, String eventType) {}

    record UserEvent(String username, String eventType, String timestamp, String systemIp) {}
}
