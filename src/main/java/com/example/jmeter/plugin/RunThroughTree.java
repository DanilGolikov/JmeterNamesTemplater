package com.example.jmeter.plugin;

import com.example.jmeter.plugin.utils.customCounter;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.jmeter.plugin.utils.GetNodeData.*;
import static com.example.jmeter.plugin.utils.RenameUtils.replaceVariables;


public class RunThroughTree {

    public static JsonNode renameConfig;
    public static JsonNode replaceBlock;
    public static boolean reloadAllTree;
    public static boolean printDebug;

    public static Map<String, String> globalVariables = new HashMap<>();
    public static Map<String, customCounter> globalCounters = new HashMap<>();

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
        printDebug = renameConfig.get("debugEnable") != null && renameConfig.get("debugEnable").asBoolean(); // default = false
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
                Long counterStart = values.get("startValue") == null ? 0L : values.get("startValue").asLong();
                Long counterEnd = values.get("endValue") == null ? null : values.get("endValue").asLong();
                int counterIncrement = values.get("increment") == null ? 1 : values.get("increment").asInt();

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
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String currentNodeType = treeNode.getTestElement().getClass().getSimpleName();
        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(currentNodeType);

        globalCounters.putIfAbsent(Integer.toString(level), new customCounter(0L, null, 1));
        logDebugOut.append(String.format("%d: %s\"%s\" (%s)\n",
                    level,
                    "|    ".repeat(level),
                    treeNode.getName(),
                    currentNodeType
        ));

        if (nodeProps != null) {
            JsonNode skipDisabled = nodeProps.get("skipDisabled");
            JsonNode searchBlock = nodeProps.get("search");
            JsonNode conditionsBlock = nodeProps.get("conditions");
            JsonNode disableJmeterVars = nodeProps.get("disableJmeterVars");
            JsonNode debugPrintConditionsResult = nodeProps.get("debugPrintConditionsResult");

            Map<String, String> nodeVariables = new HashMap<>();
            String template = nodeProps.get("template") == null ? null : nodeProps.get("template").asText();

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
                    String searchIn = shortReplaceVariable.apply(searchParam.get("searchIn").asText());
                    String searchReg = searchParam.get("searchReg").asText();
                    String searchOutVar = searchParam.get("searchOutVar").asText();
                    String searchDefault = searchParam.get("searchDefault").asText();
                    int searchRegGroup = searchParam.get("searchRegGroup").asInt();
                    JsonNode leftRightSymbols = searchParam.get("leftRightSymbols");
                    String left = (leftRightSymbols != null) ? leftRightSymbols.get(0).asText() : "";
                    String right = (leftRightSymbols != null) ? leftRightSymbols.get(1).asText() : "";

                    Pattern searchPattern = Pattern.compile(searchReg);
                    Matcher matcher = searchPattern.matcher(searchIn);
                    String result = searchDefault;
                    if (matcher.find())
                        result = left + matcher.group(searchRegGroup) + right;

                    shortVarPut.accept(searchOutVar, result);
                }
            }

            if (conditionsBlock != null) {
                for (JsonNode condition : conditionsBlock) {
                    JsonNode inParentType = condition.get("inParentType");
                    JsonNode currentLevel = condition.get("currentLevel");
                    JsonNode maxLevel = condition.get("maxLevel");
                    JsonNode minLevel = condition.get("minLevel");
                    JsonNode strEquals = condition.get("strEquals");
                    JsonNode strContains = condition.get("strContains");
                    JsonNode leftRightSymbols = condition.get("leftRightSymbols");
                    JsonNode skip = condition.get("skip");
                    JsonNode counterCommands = condition.get("counterCommands");
                    JsonNode putVar = condition.get("putVar");
                    JsonNode condTemplate = condition.get("template");

                    boolean bool_inParentType = inParentType == null ||
                            inParentType.asText().equals(getNodeType((JMeterTreeNode) treeNode.getParent()));
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

                    if (debugPrintConditionsResult != null && debugPrintConditionsResult.asBoolean()) {
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
                            if (putVar != null) {
                                String left = (leftRightSymbols != null) ? leftRightSymbols.get(0).asText() : "";
                                String right = (leftRightSymbols != null) ? leftRightSymbols.get(1).asText() : "";
                                shortVarPut.accept(putVar.get(0).asText(), left + shortReplaceVariable.apply(putVar.get(1).asText()) + right);
                            }
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
