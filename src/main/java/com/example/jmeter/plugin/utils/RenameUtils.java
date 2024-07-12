package com.example.jmeter.plugin.utils;

import com.example.jmeter.plugin.RenameTreeElements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class RenameUtils {
    public static void CheckCreateRenameConfig() {
        try {
            String resName = "rename-config.yaml";
            Path destPath = Paths.get(RenameTreeElements.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .resolve("../../../bin")
                    .normalize()
                    .resolve(resName);

            if (Files.notExists(destPath)) {
                try (InputStream is = RenameTreeElements.class.getResourceAsStream("/" + resName)) {
                    Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
