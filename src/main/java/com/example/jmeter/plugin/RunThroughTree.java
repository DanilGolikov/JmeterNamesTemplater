package com.example.jmeter.plugin;

import com.example.jmeter.plugin.utils.customCounter;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.example.jmeter.plugin.utils.GetNodeData.*;
import static com.example.jmeter.plugin.utils.RenameUtils.replaceVariables;


public class RunThroughTree {

    public static JsonNode renameConfig;
    public static JsonNode replaceBlock;
    public static boolean reloadAllTree;
    public static boolean printDebug;

    public static Map<String, String> globalVariables = new HashMap<>();
    public static Map<String, customCounter> globalCounters = new HashMap<>();
    public static Map<String, ArrayList<JsonNode>> counterConditions = new HashMap<>();

    private final StringBuilder logDebugOut;
    private final boolean removeEmptyVars;
    private static final Logger log = LoggerFactory.getLogger(RunThroughTree.class);

    public RunThroughTree() {

        // ---------------БАЗОВЫЕ ПЕРЕМЕННЫЕ (RunThroughTree)---------------
        logDebugOut = new StringBuilder("\n");
        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        // ---------------ПЕРЕМЕННЫЕ ГЛОБАЛЬНОГО УРОВНЯ (renameConfig)---------------
        reloadAllTree = renameConfig.get("reloadAllTree") != null && renameConfig.get("reloadAllTree").asBoolean(); // default = false
        printDebug = renameConfig.get("debugEnable") == null || renameConfig.get("debugEnable").asBoolean(); // default = true
        removeEmptyVars = renameConfig.get("removeEmptyVars") != null && renameConfig.get("removeEmptyVars").asBoolean(); // default = false
        replaceBlock = renameConfig.get("replace");
        JsonNode vars = renameConfig.get("variables");
        JsonNode counters = renameConfig.get("counters");

        // ---------------ИНИЦИАЛИЗАЦИЯ ГЛОБАЛЬНОГО УРОВНЯ (renameConfig)---------------
        if (vars != null)
            vars.fields().forEachRemaining(field -> globalVariables.put(field.getKey(), field.getValue().asText()));

        if (counters != null) {
            counters.fields().forEachRemaining(field -> {
                String counterName = field.getKey();
                JsonNode values = field.getValue();
                Long counterStart = values.get("start") == null ? 0L : values.get("start").asLong();
                Long counterEnd = values.get("end") == null ? null : values.get("end").asLong();
                int counterIncrement = values.get("increment") == null ? 1 : values.get("increment").asInt();

                JsonNode resetIf = values.get("resetIf");
                if (resetIf != null) {
                    counterConditions.put(counterName, new ArrayList<>());
                    for (JsonNode condition : resetIf)
                        counterConditions.get(counterName).add(condition);
                }
                globalCounters.put(counterName, new customCounter(counterStart, counterEnd, counterIncrement));
            });
        }

        // ---------------ПРОБЕГ ПО ДЕРЕВУ---------------
        traverseAndRenameTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();
        logDebugOut.append("-".repeat(50));


        // ---------------ПОСТ ОБРАБОТКА---------------
        if (reloadAllTree)
            GuiPackage.getInstance().getTreeModel().reload();

        if (printDebug)
            log.info(logDebugOut.toString());

        globalVariables.clear();
        globalCounters.clear();
        counterConditions.clear();
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String currentNodeType = treeNode.getTestElement().getClass().getSimpleName();
        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(currentNodeType);

//        globalCounters.putIfAbsent(Integer.toString(level), new customCounter(0L, null, 1));
        logDebugOut.append(String.format("%02d: %s\"%s\" (%s)\n",
                    level,
                    "|    ".repeat(level),
                    treeNode.getName(),
                    currentNodeType
        ));

        for (String counterName : counterConditions.keySet()) {
            for (JsonNode condition : counterConditions.get(counterName)) {
                JsonNode levelEquals = condition.get("levelEquals");
                JsonNode nodeType = condition.get("nodeType");

                boolean bool_levelEquals = levelEquals == null || StreamSupport.stream(levelEquals.spliterator(), false)
                        .anyMatch(value -> level == value.asInt());
                boolean bool_nodeType = nodeType == null || StreamSupport.stream(nodeType.spliterator(), false)
                        .anyMatch(value -> currentNodeType.equals(value.asText()));

                if (bool_levelEquals && bool_nodeType) {
                    globalCounters.get(counterName).resetAndGet();
                    break;
                }
            }
        }

        if (nodeProps != null) {
            JsonNode skipDisabled = nodeProps.get("skipDisabled");
            JsonNode disableJmeterVars = nodeProps.get("disableJmeterVars");
            JsonNode debugPrintConditionsResult = nodeProps.get("debugPrintConditionsResult");
            JsonNode searchBlock = nodeProps.get("search");
            JsonNode conditionsBlock = nodeProps.get("conditions");
            String template = nodeProps.get("template") == null ? null : nodeProps.get("template").asText();

            Map<String, String> nodeVariables = new HashMap<>();

            Function<String, String> shortReplaceVariable = (str) ->
                    replaceVariables(str, nodeVariables, globalVariables, globalCounters, level);

            BiConsumer<String, String> shortVarPut = (varName, varValue) -> {
                if (varName.startsWith("global."))
                    globalVariables.put(varName.replace("global.", ""), varValue);
                else
                    nodeVariables.put(varName, varValue);
            };

            if (skipDisabled != null && skipDisabled.asBoolean() && !treeNode.isEnabled()) // default = false
                return;

            getNodeData("parent.", (JMeterTreeNode) treeNode.getParent(), nodeVariables);
            getNodeData("", treeNode, nodeVariables);

            if (searchBlock != null) {
                for (JsonNode searchParam : searchBlock) {
                    JsonNode searchIn = searchParam.get("searchIn");
                    String searchString = shortReplaceVariable.apply(searchIn.get(0).asText());
                    String searchRegex = searchIn.get(1).asText();

                    JsonNode searchOut = searchParam.get("searchOut");
                    String searchOutVar = searchOut.get(0).asText();
                    String searchTemplate = searchOut.get(1).asText();
                    String searchDefault = searchOut.get(2).asText();

                    Pattern pattern = Pattern.compile(searchRegex);
                    Matcher matcher = pattern.matcher(searchString);
                    String result = searchDefault;
                    if (matcher.find()) {
                        for (int i = 1; i <= matcher.groupCount(); i++)
                            searchTemplate = searchTemplate.replace("$" + i, matcher.group(i));
                        result = searchTemplate;
                    }

                    shortVarPut.accept(searchOutVar, result);
                }
            }

            if (conditionsBlock != null) {
                for (JsonNode condition : conditionsBlock) {
                    JsonNode inParentType = condition.get("inParentType");
                    JsonNode strEquals = condition.get("strEquals");
                    JsonNode strContains = condition.get("strContains");
                    JsonNode minLevel = condition.get("minLevel");
                    JsonNode maxLevel = condition.get("maxLevel");
                    JsonNode currentLevel = condition.get("currentLevel");
                    JsonNode skip = condition.get("skip");
                    JsonNode counterCommands = condition.get("counterCommands");
                    JsonNode putVar = condition.get("putVar");
                    JsonNode condTemplate = condition.get("template");


                    boolean bool_inParentType = inParentType == null || StreamSupport.stream(inParentType.spliterator(), false)
                            .anyMatch(node -> node.asText().equals(getNodeType((JMeterTreeNode) treeNode.getParent())));
                    boolean bool_currentLevel = currentLevel == null ||
                            currentLevel.asInt() == level;
                    boolean bool_maxLevel = maxLevel == null ||
                            level <= maxLevel.asInt();
                    boolean bool_minLevel = minLevel == null ||
                            level >= minLevel.asInt();
                    boolean bool_strEquals = strEquals == null ||
                            shortReplaceVariable.apply(strEquals.get(0).asText())
                            .equals(shortReplaceVariable.apply(strEquals.get(1).asText()));
                    boolean bool_strContains = strContains == null ||
                            shortReplaceVariable.apply(strContains.get(0).asText())
                            .contains(shortReplaceVariable.apply(strContains.get(1).asText()));

                    if (debugPrintConditionsResult != null && debugPrintConditionsResult.asBoolean()) { // default = false
                        String tabCount = "     ".repeat(level + 1);
                        logDebugOut.append(String.format(
                            tabCount + "bool_inParentType: %s\n" +
                            tabCount + "bool_currentLevel: %s\n" +
                            tabCount + "bool_maxLevel: %s\n" +
                            tabCount + "bool_minLevel: %s\n" +
                            tabCount + "bool_strEquals: %s\n" +
                            tabCount + "bool_strContains: %s\n" +
                            tabCount + "-".repeat(10) + "\n",
                                bool_inParentType,
                                bool_currentLevel,
                                bool_maxLevel,
                                bool_minLevel,
                                bool_strEquals,
                                bool_strContains
                        ));
                    }
                    if (
                        bool_inParentType &&
                        bool_currentLevel &&
                        bool_maxLevel &&
                        bool_minLevel &&
                        bool_strEquals &&
                        bool_strContains
                    ) {
                        if (skip != null && skip.asBoolean()) // default = false
                            return;
                        if (putVar != null || condTemplate != null || counterCommands != null) {
                            if (putVar != null)
                                shortVarPut.accept(putVar.get(0).asText(), shortReplaceVariable.apply(putVar.get(1).asText()));
                            if (condTemplate != null)
                                template = condTemplate.asText();
                            if (counterCommands != null)
                                shortReplaceVariable.apply(counterCommands.asText());
                        } else
                            return;
                        break;
                    }
                }
            }

            if (template == null)
                return;

            template = shortReplaceVariable.apply(template);

            if (disableJmeterVars == null || disableJmeterVars.asBoolean()) // default = true
                template = template.replaceAll("\\$\\{", "{");
            if (removeEmptyVars)
                template = template.replaceAll("#\\{.*?}", "");
            if (replaceBlock != null)
                for (JsonNode replacement : replaceBlock) {
                    String replaceRegex = replacement.get(0).asText();
                    String replaceReplacement = replacement.get(1).asText();
                    JsonNode replaceNodeType = replacement.get(2);

                    if (replaceNodeType == null)
                        template = template.replaceAll(replaceRegex, replaceReplacement);
                    else if (currentNodeType.equals(replaceNodeType.asText()))
                        template = template.replaceAll(replaceRegex, replaceReplacement);
                }

            if (!template.equals(nodeVariables.get("name"))) {
//                log.info("No skipped Jmeter Node\nname: {}\ntemplate: {}\n----------", nodeVariables.get("name"), template);
                treeNode.setName(template);
                if (!reloadAllTree)
                    GuiPackage.getInstance().getTreeModel().nodeChanged(treeNode);
            }
        }

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            JMeterTreeNode child = (JMeterTreeNode) children.nextElement();
            traverseAndRenameTree(child, level + 1);
        }
    }
}
