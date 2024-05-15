package com.slaughtersquad;

import java.io.*;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Copying the dataset file");
            // Get the file created by the writer robot
            String pathToDatasetFileCreatedByRobot = "SlaughterSquad/target/classes/com/slaughtersquad/sampleRobots/WriterRobot.data/dataset.csv";
            Path path = Path.of(pathToDatasetFileCreatedByRobot);

            File datasetFile = new File(path.toAbsolutePath().toString());

            // Create a buffered reader to read the file line by line and insert the data into a new file
            FileReader fr = new FileReader(datasetFile);
            BufferedReader br = new BufferedReader(fr);

            // Create a new file to store the data
            String pathToDatasetFile = "SlaughterSquad\\src\\main\\java\\com\\slaughtersquad\\datasets\\dataset.csv";
            Path pathToDataset = Path.of(pathToDatasetFile);
            File dataset = new File(pathToDataset.toString());
            PrintWriter pw = new PrintWriter(dataset);

            String line;

            // Read line by line and print it to the new file
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }

            // Close everything
            pw.flush();
            pw.close();

            fr.close();
            br.close();

            // Delete the file created by the robot immediately after copying
            if (datasetFile.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}