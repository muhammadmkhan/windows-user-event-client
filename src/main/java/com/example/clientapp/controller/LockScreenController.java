package com.example.clientapp.controller;

import com.example.clientapp.config.ApplicationContextProvider;
import com.example.clientapp.config.PropertiesInfo;
import com.example.clientapp.util.KioskLockUtil;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class LockScreenController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private WebClient webClient;

    @Autowired
    private PropertiesInfo propertiesInfo;

    @PostConstruct
    public void init() {
        String ip = propertiesInfo.getMainServerIp();
        String port = propertiesInfo.getServerPort();

        if (ip == null || ip.isBlank() || port == null || port.isBlank()) {
            throw new IllegalStateException("Server IP or Port not configured properly");
        }

        this.webClient = WebClient.builder()
                .baseUrl("http://"+propertiesInfo.getMainServerIp()+":"+propertiesInfo.getServerPort()) // POS API endpoint
                .build();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (propertiesInfo.getKillMe().equals(username) && propertiesInfo.getKillMePass().equals(password)) {
            messageLabel.setStyle("-fx-text-fill: orange;");
            messageLabel.setText("Master override activated!");

            new Thread(() -> {
                try {
                    // 1. Stop the scheduled task (replace "MyAppTask" with your Task Scheduler task name)
                    ProcessBuilder stopTask = new ProcessBuilder(
                            "cmd", "/c", "schtasks /end /tn \"possys-client\" /f"
                    );
                    stopTask.start().waitFor();

                    KioskLockUtil.disableKioskMode();

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> messageLabel.setText("Failed to execute override."));
                }
            }).start();

            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both fields.");
            return;
        }

        webClient.post()
                .uri("/api/customer/auth")
                .bodyValue(Map.of("username", username, "password", password))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    Platform.runLater(() ->
                            messageLabel.setText("Connection failed: " + e.getMessage()));
                    return Mono.empty();
                })
                .subscribe(response -> Platform.runLater(() -> {
                    if (response != null && response.contains("success")) {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Access Granted!");
                        openMainScreen();
                    } else {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Invalid credentials.");
                    }
                }));
    }

    private void openMainScreen() {
        try {
//            KioskLockUtil.disableKioskMode();
            Stage stage = (Stage) usernameField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            loader.setControllerFactory(ApplicationContextProvider.getContext()::getBean);

            Scene mainScene = new Scene(loader.load());
            stage.setScene(mainScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
