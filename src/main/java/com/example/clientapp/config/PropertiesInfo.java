package com.example.clientapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.info")
public class PropertiesInfo {


    private String mainServerIp;
    private String serverPort;


    public String getMainServerIp() {
        return mainServerIp;
    }

    public void setMainServerIp(String mainServerIp) {
        this.mainServerIp = mainServerIp;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }
}
