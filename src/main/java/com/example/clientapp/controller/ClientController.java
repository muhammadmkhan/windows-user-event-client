package com.example.clientapp.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client")
public class ClientController {


    @GetMapping("/signOut")
    public void SigOut(){
        try {
            // Execute Windows logoff command
            ProcessBuilder pb = new ProcessBuilder("shutdown", "/l");
            pb.inheritIO();
            Process process = pb.start();

            // Optionally wait for completion
            process.waitFor();

            System.out.print("Logoff command executed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("Failed to log out Windows user: " + e.getMessage());
        }
    }

    @GetMapping("/isAlive")
    public ResponseEntity<?> isAlive(){
    return ResponseEntity.ok("Breathing");
    }

}
