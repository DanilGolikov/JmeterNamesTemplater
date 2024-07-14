package com.example.jmeter.plugin.utils;

import com.example.jmeter.plugin.RunThroughTree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class RenameUtils {

    public static JsonNode renameConfig;

    public static void CheckCreateRenameConfig() {
        try {
            String resName = "rename-config.json";
            Path destPath = Paths.get(RunThroughTree.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .resolve("../../../bin")
                    .normalize()
                    .resolve(resName);

            if (Files.notExists(destPath)) {
                try (InputStream is = RunThroughTree.class.getResourceAsStream("/" + resName)) {
                    assert is != null;
                    Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            renameConfig = mapper.readTree(new File(destPath.toString()));
            RunThroughTree.renameConfig = renameConfig;
//            System.out.println(renameConfig.toPrettyString());


        } catch (IOException | URISyntaxException e) {
            System.out.println(e);;
        }
    }

    public static String replaceVariables(String str, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            str = str.replace(placeholder, entry.getValue());
        }
        return str;
    }
}
