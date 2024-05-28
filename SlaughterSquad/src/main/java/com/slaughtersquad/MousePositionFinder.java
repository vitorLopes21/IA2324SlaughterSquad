package com.slaughtersquad;

import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MousePositionFinder {
    private static final String MOUSE_COORDS_FILE_PATH = "SlaughterSquad/libs/mouse_coords.txt";
    private static Point lastPoint = null;

    public static void main(String[] args) {
        try {
            Path mouseCoordsFilePath = Paths.get(MOUSE_COORDS_FILE_PATH);
            File file = mouseCoordsFilePath.toFile();

            if (!file.exists()) {
                System.out.println("Creating mouse coordinates file...");
                file.createNewFile();
            }

            while (true) {
                Point point = MouseInfo.getPointerInfo().getLocation();

                System.out.println("X: " + point.x + ", Y: " + point.y);

                if (!point.equals(lastPoint)) {
                    lastPoint = point;

                    System.out.println("Writing mouse coordinates to file...");
                    try (FileWriter fileWriter = new FileWriter(file, false);
                         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                        bufferedWriter.write(point.x + ";" + point.y);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
