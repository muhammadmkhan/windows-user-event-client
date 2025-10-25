package com.example.clientapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class ClientService {
    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    public void forceLogoffActiveUser() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-Command",
                    "$session = (query user | Select-String -Pattern '\\s+(\\d+)\\s+Active').Matches.Groups[1].Value; " +
                            "if ($session) { logoff $session /v } else { Write-Host 'No active session found' }"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(System.out::println);
            }

            int exitCode = process.waitFor();
            log.info("Logoff command executed, exit code = " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
