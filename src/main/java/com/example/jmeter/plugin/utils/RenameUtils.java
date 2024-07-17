package com.example.jmeter.plugin.utils;

import com.example.jmeter.plugin.RunThroughTree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameUtils {

    public static JsonNode renameConfig;

    private static final Logger log = LoggerFactory.getLogger(RenameUtils.class);

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
            log.debug(renameConfig.toPrettyString());


        } catch (IOException | URISyntaxException e) {
            log.error(e.toString());;
        }
    }

    public static String replaceVariables(
                                    String str,
                                    Map<String, String> nodeVariables,
                                    Map<String, String> globalVariables,
                                    Map<String, customCounter> counters,
                                    int level) {

        for (Map.Entry<String, String> entry : nodeVariables.entrySet())
            str = str.replace("#{" + entry.getKey() + "}", entry.getValue());

        for (Map.Entry<String, String> entry : globalVariables.entrySet())
            str = str.replace("#{global." + entry.getKey() + "}", entry.getValue());

        Pattern counterPattern = Pattern.compile("#\\{__counter\\((.+?)\\)}");
        Matcher counterMatcher = counterPattern.matcher(str);
        while (counterMatcher.find()) {
            String placeHolder = counterMatcher.group();
            String[] counterParams = counterMatcher.group(1).split(",", -1);
            String counterName = counterParams[0];
            String counterCommand = counterParams[1];
            String counterFormat = counterParams[2];
            String counterIndex;
            long counterValue;

            switch (counterName) {
                case "current":
                    counterIndex = Integer.toString(level);
                    break;
                case "parent":
                    counterIndex = Integer.toString(level - 1);
                    break;
                default:
                    counterIndex = counterName;
            }

            switch (counterCommand) {
                case "get":
                    counterValue = counters.get(counterIndex).get();
                    break;
                case "resetAndGet":
                    counterValue = counters.get(counterIndex).resetAndGet();
                    break;
                default:
                    counterValue = counters.get(counterIndex).addAndGet(1L);
            }

            str = str.replace(placeHolder, String.format("%" + counterFormat +  "d", counterValue));
        }

        return str;
    }
}
