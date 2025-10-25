package com.example.clientapp.controller;


import com.example.clientapp.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/client")
public class ClientController {


    @Autowired
    ClientService service;
    @GetMapping("/signOut")
    public void SigOut(){
        try {
            service.forceLogoffActiveUser();

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
