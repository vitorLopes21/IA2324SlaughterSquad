package com.slaughtersquad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;

public class Monitor {
    private static final String SIGNAL_FILE_PATH = "SlaughterSquad/target/classes/com/slaughtersquad/sampleRobots/WriterRobot.data/battle_finished_signal.txt";
    private static final String CLASS_PATH = "SlaughterSquad/target/classes";
    private static final String MAIN_CLASS = "com.slaughtersquad.Main";

    private static FileTime lastModifiedTime = FileTime.fromMillis(0);

    public static void main(String[] args) {
        Path signalFilePath = Paths.get(SIGNAL_FILE_PATH);

        try (WatchService watchService = signalFilePath.getParent().getFileSystem().newWatchService()) {
            signalFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                            || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changed = (Path) event.context();
                        if (changed.endsWith(signalFilePath.getFileName())) {
                            FileTime currentModifiedTime = Files.getLastModifiedTime(signalFilePath);
                            if (!currentModifiedTime.equals(lastModifiedTime)) {
                                // Run the Main class
                                runMainClass();

                                // Reset the watch key
                                key.reset();
                                break;
                            }
                        }
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runMainClass() {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", CLASS_PATH, MAIN_CLASS);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
