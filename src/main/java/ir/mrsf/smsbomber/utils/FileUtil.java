package ir.mrsf.smsbomber.utils;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class FileUtil {

    public List<String> readDomains(String filePath) throws IOException {
        final List<String> domains = new ArrayList<>();
        final Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    domains.add(line);
                }
            }
        }

        return domains;
    }

    public void writeDomains(String filePath, List<String> domains) throws IOException {
        final Path path = Paths.get(filePath);

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String domain : domains) {
                writer.write(domain);
                writer.newLine();
            }
        }

        System.out.println("Domains saved to: " + path.toAbsolutePath());
    }
} 