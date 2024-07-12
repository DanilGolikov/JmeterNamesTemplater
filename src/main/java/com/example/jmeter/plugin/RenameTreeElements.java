package com.example.jmeter.plugin;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

import java.util.Enumeration;


public class RenameTreeElements {

    public RenameTreeElements() {

        JMeterTreeModel jMeterTreeModel = GuiPackage.getInstance().getTreeModel();
        JMeterTreeNode firstNode = jMeterTreeModel.getNodesOfType(Object.class).get(1);

        traverseTree(firstNode, 0);
        GuiPackage.getInstance().refreshCurrentGui();
        GuiPackage.getInstance().getMainFrame().repaint();
    }

    private void traverseTree(JMeterTreeNode treeNode, int level) {
        System.out.println("    ".repeat(level) + treeNode.getName());
        treeNode.setName(treeNode.getName() + " changed");
        GuiPackage.getInstance().getTreeModel().nodeChanged(treeNode);

        Enumeration<?> children = treeNode.children();
        while (children.hasMoreElements()) {
            JMeterTreeNode child = (JMeterTreeNode) children.nextElement();
            traverseTree(child, level + 1);
        }
    }

}
