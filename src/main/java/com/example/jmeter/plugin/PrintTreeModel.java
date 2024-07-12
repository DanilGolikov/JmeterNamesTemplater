package com.example.jmeter.plugin;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.Searchable;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PrintTreeModel {
    public PrintTreeModel() {
        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);
        traverseTree(firstNode, 0);
    }

    private void traverseTree(JMeterTreeNode treeNode, int level) {
        System.out.println("    ".repeat(level) + treeNode.getName());
        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            JMeterTreeNode child = (JMeterTreeNode) children.nextElement();
            traverseTree(child, level + 1);
        }
    }
}
