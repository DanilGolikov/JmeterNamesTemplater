package com.example.jmeter.plugin;

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

    public RunThroughTree() {
        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        traverseAndRenameTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();
//        GuiPackage.getInstance().getTreeModel().reload();
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String currentNodeType = treeNode.getTestElement().getClass().getSimpleName();
        System.out.println(level + "    ".repeat(level) + currentNodeType);

        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(currentNodeType);
        if (nodeProps != null) {
            Map<String, String> variables = new HashMap<>();
            getNodeData("parent.", (JMeterTreeNode) treeNode.getParent(), variables);
            getNodeData("", treeNode, variables);
            String template = nodeProps.get("template").asText();

            JsonNode searchBlock = nodeProps.get("search");
            if (searchBlock != null) {
                for (JsonNode searchParam : searchBlock) {
                    String searchIn = replaceVariables(searchParam.get("searchIn").asText(), variables);
                    String searchReg = searchParam.get("searchReg").asText();
                    String searchOut = searchParam.get("searchOut").asText();
                    String searchDefault = searchParam.get("searchDefault").asText();
                    int searchRegGroup = searchParam.get("searchRegGroup").asInt();

                    Pattern searchPattern = Pattern.compile(searchReg);
                    Matcher matcher = searchPattern.matcher(searchIn);
                    String result = searchDefault;
                    if (matcher.find()) {
                        result = matcher.group(searchRegGroup);
                    }
                    variables.put(searchOut, result);
                }
            }

            JsonNode conditionsBlock = nodeProps.get("conditions");
            if (conditionsBlock != null) {
                for (JsonNode condition : conditionsBlock) {
                    String inParentType = condition.get("inParentType").asText();
                    JsonNode maxLevel = condition.get("maxLevel");
                    if (inParentType.equals(getNodeType((JMeterTreeNode) treeNode.getParent())) &&
                            (maxLevel == null || (level <= maxLevel.asInt())))
                        template = condition.get("template").asText();
                }
            }

            treeNode.setName(replaceVariables(template, variables));
            GuiPackage.getInstance().getTreeModel().nodeChanged(treeNode);
        }

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            JMeterTreeNode child = (JMeterTreeNode) children.nextElement();
            traverseAndRenameTree(child, level + 1);
        }
    }
}
