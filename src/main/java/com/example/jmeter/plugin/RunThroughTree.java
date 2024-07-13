package com.example.jmeter.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.example.jmeter.plugin.utils.GetNodeData.getCurrentNodeData;
import static com.example.jmeter.plugin.utils.GetNodeData.getParentNodeData;
import static com.example.jmeter.plugin.utils.RenameUtils.replaceVariables;


public class RunThroughTree {

    public static JsonNode renameConfig;

    public RunThroughTree() {
        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        traverseAndRenameTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();
    }

    private void traverseAndRenameTree(JMeterTreeNode treeNode, int level) {
        String testElementType = treeNode.getTestElement().getClass().getSimpleName();
        System.out.println("    ".repeat(level) + testElementType);

        JsonNode nodeProps = renameConfig.get("NodeProperties").findValue(testElementType);
        if (nodeProps != null) {
            String template = nodeProps.get("template").asText();

            Map<String, String> variables = new HashMap<>();
            getParentNodeData(treeNode, variables);
            getCurrentNodeData("", treeNode, variables);
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
