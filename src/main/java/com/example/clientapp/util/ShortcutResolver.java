package com.example.clientapp.util;

import com.example.clientapp.tests.WindowsShortcut;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ShortcutResolver {

    /**
     * Returns details of any file (.lnk or .exe)
     * Keys: TargetPath, Description, Arguments, WorkingDirectory
     */
    public static Map<String, String> getFileDetails(File file) {
        Map<String, String> details = new HashMap<>();

        if (file == null || !file.exists()) {
            details.put("Error", "File not found");
            return details;
        }

        try {
            String filePath = file.getAbsolutePath();

            // Case 1: Windows Shortcut (.lnk)
            if (filePath.toLowerCase().endsWith(".lnk")) {
                String command = String.format(
                        "powershell -Command \"Start-Process powershell -Verb RunAs -ArgumentList '-Command \"$s=(New-Object -ComObject WScript.Shell).CreateShortcut('%s'); " +
                                "Write-Output ('TargetPath=' + $s.TargetPath); " +
                                "Write-Output ('Description=' + $s.Description); " +
                                "Write-Output ('Arguments=' + $s.Arguments); " +
                                "Write-Output ('WorkingDirectory=' + $s.WorkingDirectory)\"'\"",
                        filePath.replace("\\", "\\\\")
                );

                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("=")) {
                            String[] parts = line.split("=", 2);
                            details.put(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
                        }
                    }
                }

                process.waitFor();

                // Fallback in case PowerShell returned empty
                if (!details.containsKey("TargetPath") || details.get("TargetPath").isEmpty()) {
                    details.put("TargetPath", filePath);
                    details.put("Description", "Unknown Shortcut");
                }

            }
            // Case 2: Executable (.exe)
            else if (filePath.toLowerCase().endsWith(".exe")) {
                details.put("TargetPath", filePath);
                details.put("WorkingDirectory", file.getParent());
                details.put("Description", getExeDescription(filePath));
                details.put("Arguments", "");
            }
            // Case 3: Other (like .bat or other launchers)
            else {
                details.put("TargetPath", filePath);
                details.put("WorkingDirectory", file.getParent());
                details.put("Description", "Batch or unknown launcher");
                details.put("Arguments", "");
            }

        } catch (Exception e) {
            details.put("Error", e.getMessage());
            e.printStackTrace();
        }

        return details;
    }

    /**
     * Extracts the description (File Version Info) from an .exe file via PowerShell
     */
    private static String getExeDescription(String exePath) {
        try {
            String command = String.format(
                    "powershell -Command \"(Get-Item '%s').VersionInfo | Select-Object -ExpandProperty FileDescription\"",
                    exePath.replace("\\", "\\\\")
            );

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String description = reader.readLine();
                if (description != null && !description.isEmpty()) {
                    return description.trim();
                }
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Executable";
    }

    // 🧪 Test locally
    public static void main(String[] args) throws IOException, ParseException {
        File file1 = new File("C:\\Games\\Counter-strike 1.6 Original.lnk");
        File file2 = new File("C:\\Games\\steam.lnk");

        System.out.println("Shortcut Info:");
        System.out.println(getFileDetails(file1));

        System.out.println("\nExecutable Info:");
        System.out.println(getFileDetails(file2));

        WindowsShortcut windowsShortcut = new WindowsShortcut(file2);
        System.out.println("\nWindows Shortcut:");
        System.out.println(windowsShortcut.getDescription());
        System.out.println(windowsShortcut.getRealFilename());
    }
}
