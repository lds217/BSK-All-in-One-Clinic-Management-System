package BsK.client.ui.component.CheckUpPage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class DebugFileWalk {

    public static void main(String[] args) {
        // ▼▼▼ IMPORTANT ▼▼▼
        // Change this path to the EXACT folder you are trying to scan.
        // Use double backslashes "\\" for Windows paths.
        // Example: "C:\\Users\\YourName\\Pictures\\MyEvent"
        Path mediaPath = Paths.get("ANH SIEU AM");
        // ▲▲▲ IMPORTANT ▲▲▲


        System.out.println("--- Starting Scan ---");
        System.out.println("Scanning Path: " + mediaPath.toAbsolutePath());
        System.out.println("Does path exist? " + Files.exists(mediaPath));
        System.out.println("--------------------------------\n");

        System.out.println(">>> ALL files and folders found by Files.walk():");
        try (Stream<Path> pathStream = Files.walk(mediaPath)) {
            pathStream.forEach(path -> {
                System.out.println("  -> Found: " + path);
            });
        } catch (IOException e) {
            System.err.println("An ERROR occurred during the scan:");
            e.printStackTrace();
        }

        System.out.println("\n--------------------------------");
        System.out.println(">>> Files that match the '.jpg' filter:");
        try (Stream<Path> pathStream = Files.walk(mediaPath)) {
             pathStream
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> {
                    String lowerName = path.toString().toLowerCase();
                    return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");
                })
                .forEach(path -> {
                    System.out.println("  -> Matched: " + path);
                });
        } catch (IOException e) {
            System.err.println("An ERROR occurred during the filtered scan:");
            e.printStackTrace();
        }

         System.out.println("\n--- Scan Finished ---");
    }
}