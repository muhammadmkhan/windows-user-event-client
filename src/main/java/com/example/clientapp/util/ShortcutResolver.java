package com.example.clientapp.util;

import java.io.File;


public class ShortcutResolver {


    public static String resolveShortcutTarget(File shortcutFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-Command",
                    "(New-Object -COM WScript.Shell).CreateShortcut('"
                            + shortcutFile.getAbsolutePath() + "').TargetPath"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.InputStream is = process.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String target = s.hasNext() ? s.next().trim() : null;
            process.waitFor();
            return target;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
