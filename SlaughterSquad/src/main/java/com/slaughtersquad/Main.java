package com.slaughtersquad;

import java.io.*;
import java.nio.file.Path;

public class Main {
    private static String FILE_CREATED_BY_ROBOT = "SlaughterSquad/target/classes/com/slaughtersquad/sampleRobots/WriterRobot.data/dataset.csv";
    private static String FILE_TO_COPY = "SlaughterSquad/src/main/java/com/slaughtersquad/datasets/dataset.csv";

    public static void main(String[] args) {
        try {
            System.out.println("Copying the dataset file");
            // Get the file created by the writer robot
            Path path = Path.of(FILE_CREATED_BY_ROBOT);

            File datasetFile = new File(path.toAbsolutePath().toString());

            // Create a buffered reader to read the file line by line and insert the data into a new file
            FileReader fr = new FileReader(datasetFile);
            BufferedReader br = new BufferedReader(fr);

            // Create a new file to store the data
            Path pathToDataset = Path.of(FILE_TO_COPY);
            File dataset = new File(pathToDataset.toString());

            // Create a file writer to write the data to the new file
            FileWriter fw = new FileWriter(dataset, true);
            PrintWriter pw = new PrintWriter(fw);

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