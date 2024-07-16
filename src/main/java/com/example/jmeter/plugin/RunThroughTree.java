package com.example.jmeter.plugin;

import com.example.jmeter.plugin.utils.customCounter;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.jmeter.plugin.utils.GetNodeData.*;
import static com.example.jmeter.plugin.utils.RenameUtils.replaceVariables;


public class RunThroughTree {

    public static JsonNode renameConfig;
    public static boolean printTree;
    public static boolean reloadAllTree;
    public static Map<String, customCounter> counters = new HashMap<>();

    public RunThroughTree() {
        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        reloadAllTree = renameConfig.get("reloadAllTree") != null && renameConfig.get("reloadAllTree").asBoolean();
        printTree = renameConfig.get("printTree") != null && renameConfig.get("printTree").asBoolean();

        traverseAndRenameTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();

        if (printTree)
            System.out.println("-".repeat(50));

        if (reloadAllTree)
            GuiPackage.getInstance().getTreeModel().reload();

        counters.clear();
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String currentNodeType = treeNode.getTestElement().getClass().getSimpleName();
        counters.putIfAbsent(Integer.toString(level), new customCounter(0L));
        if (printTree)
            System.out.printf("%s: %s\"%s\" (%s)%n",
                    level,
                    "|    ".repeat(level),
                    treeNode.getName(),
                    currentNodeType
            );

        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(currentNodeType);
        if (nodeProps != null) {
            Map<String, String> variables = new HashMap<>();
            getNodeData("parent.", (JMeterTreeNode) treeNode.getParent(), variables);
            getNodeData("", treeNode, variables);
//            getNodeData("next", (JMeterTreeNode) treeNode.children().nextElement(), variables);
            String template = nodeProps.get("template").asText();

            JsonNode searchBlock = nodeProps.get("search");
            if (searchBlock != null) {
                for (JsonNode searchParam : searchBlock) {
                    String searchIn = replaceVariables(
                            searchParam.get("searchIn").asText(),
                            variables,
                            counters,
                            level);
                    String searchReg = searchParam.get("searchReg").asText();
                    String searchOutVar = searchParam.get("searchOutVar").asText();
                    String searchDefault = searchParam.get("searchDefault").asText();
                    int searchRegGroup = searchParam.get("searchRegGroup").asInt();

                    Pattern searchPattern = Pattern.compile(searchReg);
                    Matcher matcher = searchPattern.matcher(searchIn);
                    String result = searchDefault;
                    if (matcher.find())
                        result = matcher.group(searchRegGroup);
                    variables.put(searchOutVar, result);
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
                            replaceVariables(
                                    strEquals.get(0).asText(),
                                    variables,
                                    counters,
                                    level)
                            .equals(
                            replaceVariables(strEquals.get(1).asText(),
                                    variables,
                                    counters,
                                    level));
                    boolean bool_strContains = strContains == null ||
                            replaceVariables(
                                    strContains.get(0).asText(),
                                    variables,
                                    counters,
                                    level)
                            .contains(
                            replaceVariables(strContains.get(1).asText(),
                                    variables,
                                    counters,
                                    level));

                    String tabCount = "     ".repeat(level+1);
                    System.out.printf(
                            tabCount + "bool_inParentType: %s%n" +
                            tabCount + "bool_currentLevel: %s%n" +
                            tabCount + "bool_maxLevel: %s%n" +
                            tabCount + "bool_strEquals: %s%n" +
                            tabCount + "bool_strContains: %s%n" +
                            tabCount + "-".repeat(10) + "%n",
                            bool_inParentType,
                            bool_currentLevel,
                            bool_maxLevel,
                            bool_strEquals,
                            bool_strContains
                    );
                    if (
                        bool_inParentType &&
                        bool_currentLevel &&
                        bool_maxLevel &&
                        bool_strEquals &&
                        bool_strContains
                    ) {
                        template = condition.get("template").asText();
                        break;
                    }

                }
            }

            JsonNode replaceJmeterVars = nodeProps.get("replaceJmeterVars");
            if (replaceJmeterVars != null && replaceJmeterVars.asBoolean())
                template = template.replaceAll("\\$\\{", "{");
            treeNode.setName(replaceVariables(
                    template,
                    variables,
                    counters,
                    level));
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
