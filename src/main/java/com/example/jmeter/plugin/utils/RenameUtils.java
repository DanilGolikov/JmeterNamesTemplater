package com.example.jmeter.plugin.utils;

import com.example.jmeter.plugin.RunThroughTree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;


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
            String resName = "rename-config.yaml";
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
            Map<String, Object> map = new Yaml().load(Files.newInputStream(destPath));
            renameConfig = new ObjectMapper().valueToTree(map);
            RunThroughTree.renameConfig = renameConfig;
            log.debug(renameConfig.toPrettyString());


        } catch (IOException | URISyntaxException e) {
            log.error(e.toString());
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

        Pattern counterPattern = Pattern.compile("#\\{([^}]*)\\((.*?)\\)}");
        Matcher counterMatcher = counterPattern.matcher(str);
        while (counterMatcher.find()) {
            String placeHolder = counterMatcher.group();
            String counterName = counterMatcher.group(1);
            String[] counterParams = counterMatcher.group(2).split(",", -1);
            String counterCommand = counterParams.length > 0 ? counterParams[0] : "addAndGet";
            String counterFormat = counterParams.length > 1 ? counterParams[1] : "";
            long counterValue;
            log.debug("Counter name: {} | counter command: {}", counterName, counterCommand);

//            switch (counterName) {
//                case "current":
//                    counterName = Integer.toString(level);
//                    break;
//                case "parent":
//                    counterName = Integer.toString(level - 1);
//                    break;
//            }

            try {
                log.debug("Counter value before replace: {}", counters.get(counterName).get());
                switch (counterCommand) {
                    case "get":
                        counterValue = counters.get(counterName).get();
                        break;
                    case "resetAndGet":
                        counterValue = counters.get(counterName).resetAndGet();
                        break;
                    case "getAndAdd":
                        counterValue = counters.get(counterName).getAndAdd();
                        break;
                    case "addAndGet":
                    default:
                        counterValue = counters.get(counterName).addAndGet();
                }
                log.debug("Counter value after replace: {}", counters.get(counterName).get());
                str = str.replace(placeHolder, String.format("%" + counterFormat +  "d", counterValue));
            } catch (NullPointerException ignore) {}
        }
        return str;
    }
}
