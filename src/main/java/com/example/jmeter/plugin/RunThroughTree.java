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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.jmeter.plugin.utils.GetNodeData.*;
import static com.example.jmeter.plugin.utils.RenameUtils.replaceVariables;


public class RunThroughTree {

    public static JsonNode renameConfig;
    public static boolean reloadAllTree;
    public static boolean printDebug;

    public static Map<String, String> globalVariables = new HashMap<>();
    public static Map<String, customCounter> globalCounters = new HashMap<>();

    private final StringBuilder logDebugOut;
    private static final Logger log = LoggerFactory.getLogger(RunThroughTree.class);

    public RunThroughTree() {
        logDebugOut = new StringBuilder("\n");

        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        reloadAllTree = renameConfig.get("reloadAllTree") != null && renameConfig.get("reloadAllTree").asBoolean();
        printDebug = renameConfig.get("printDebug") != null && renameConfig.get("printDebug").asBoolean();

        JsonNode vars = renameConfig.get("variables");
        if (globalVariables != null) {
            vars.fields().forEachRemaining(field -> {
                globalVariables.put(field.getKey(), field.getValue().asText());
            });
        }

        traverseAndRenameTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();

        logDebugOut.append("-".repeat(50));

        if (reloadAllTree)
            GuiPackage.getInstance().getTreeModel().reload();

        if (printDebug)
            log.info(logDebugOut.toString());

        globalVariables.clear();
        globalCounters.clear();
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String currentNodeType = treeNode.getTestElement().getClass().getSimpleName();
        globalCounters.putIfAbsent(Integer.toString(level), new customCounter(0L));
        logDebugOut.append(String.format("%d: %s\"%s\" (%s)\n",
                    level,
                    "|    ".repeat(level),
                    treeNode.getName(),
                    currentNodeType
        ));

        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(currentNodeType);
        if (nodeProps != null) {
            Map<String, String> nodeVariables = new HashMap<>();
            Function<String, String> shortReplaceVariable = (str) ->
                    replaceVariables(str, nodeVariables, globalVariables, globalCounters, level);

            BiConsumer<String, String> shortVarPut = (varName, varValue) -> {
                if (varName.startsWith("global."))
                    globalVariables.put(varName.replace("global.", ""), varValue);
                else
                    nodeVariables.put(varName, varValue);
            };

            getNodeData("parent.", (JMeterTreeNode) treeNode.getParent(), nodeVariables);
            getNodeData("", treeNode, nodeVariables);
//            getNodeData("next", (JMeterTreeNode) treeNode.children().nextElement(), nodeVariables);
            String template = nodeProps.get("template").asText();

            JsonNode searchBlock = nodeProps.get("search");
            if (searchBlock != null) {
                for (JsonNode searchParam : searchBlock) {
                    String searchIn = shortReplaceVariable.apply(searchParam.get("searchIn").asText());
                    String searchReg = searchParam.get("searchReg").asText();
                    String searchOutVar = searchParam.get("searchOutVar").asText();
                    String searchDefault = searchParam.get("searchDefault").asText();
                    int searchRegGroup = searchParam.get("searchRegGroup").asInt();

                    Pattern searchPattern = Pattern.compile(searchReg);
                    Matcher matcher = searchPattern.matcher(searchIn);
                    String result = searchDefault;
                    if (matcher.find())
                        result = matcher.group(searchRegGroup);

                    shortVarPut.accept(searchOutVar, result);
                }
            }

            JsonNode conditionsBlock = nodeProps.get("conditions");
            if (conditionsBlock != null) {
                for (JsonNode condition : conditionsBlock) {
                    JsonNode inParentType = condition.get("inParentType");
                    JsonNode currentLevel = condition.get("currentLevel");
                    JsonNode maxLevel = condition.get("maxLevel");
                    JsonNode strEquals = condition.get("strEquals");
                    JsonNode strContains = condition.get("strContains");

                    boolean bool_inParentType = inParentType == null ||
                            inParentType.asText().equals(getNodeType((JMeterTreeNode) treeNode.getParent()));
                    boolean bool_currentLevel = currentLevel == null ||
                            currentLevel.asInt() == level;
                    boolean bool_maxLevel = maxLevel == null ||
                            maxLevel.asInt() <= level;
                    boolean bool_strEquals = strEquals == null ||
                            shortReplaceVariable.apply(strEquals.get(0).asText())
                            .equals(shortReplaceVariable.apply(strEquals.get(1).asText()));
                    boolean bool_strContains = strContains == null ||
                            shortReplaceVariable.apply(strContains.get(0).asText())
                            .contains(shortReplaceVariable.apply(strContains.get(1).asText()));

                    String tabCount = "     ".repeat(level+1);
                    logDebugOut.append(String.format(
                            tabCount + "bool_inParentType: %s\n" +
                            tabCount + "bool_currentLevel: %s\n" +
                            tabCount + "bool_maxLevel: %s\n" +
                            tabCount + "bool_strEquals: %s\n" +
                            tabCount + "bool_strContains: %s\n" +
                            tabCount + "-".repeat(10) + "\n",
                            bool_inParentType,
                            bool_currentLevel,
                            bool_maxLevel,
                            bool_strEquals,
                            bool_strContains
                    ));
                    if (
                        bool_inParentType &&
                        bool_currentLevel &&
                        bool_maxLevel &&
                        bool_strEquals &&
                        bool_strContains
                    ) {
                        JsonNode putVar = condition.get("putVar");
                        if (putVar != null)
                            shortVarPut.accept(putVar.get(0).asText(), shortReplaceVariable.apply(putVar.get(1).asText()));

                        template = condition.get("template").asText();
                        break;
                    }

                }
            }

            JsonNode replaceJmeterVars = nodeProps.get("replaceJmeterVars");
            if (replaceJmeterVars != null && replaceJmeterVars.asBoolean())
                template = template.replaceAll("\\$\\{", "{");
            treeNode.setName(shortReplaceVariable.apply(template));
        }
        if (!reloadAllTree)
            GuiPackage.getInstance().getTreeModel().nodeChanged(treeNode);

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            JMeterTreeNode child = (JMeterTreeNode) children.nextElement();
            traverseAndRenameTree(child, level + 1);
        }
    }
}
