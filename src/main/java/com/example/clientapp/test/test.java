package com.example.clientapp.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class test {

    public static String getCurrentUser() {
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe",
                    "(Get-WmiObject -Class Win32_ComputerSystem).UserName");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String username = reader.readLine();
                if (username != null && !username.isEmpty()) {
                    return username.trim();
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) {
        System.out.println(getCurrentUser());
    }
}
