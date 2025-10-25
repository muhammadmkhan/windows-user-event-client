package com.example.clientapp;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApp {

    public static void main(String[] args) {
        // Start JavaFX app, which will start Spring context internally
        Application.launch(ClientFxApp.class, args);
    }
}
