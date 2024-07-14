package com.example.jmeter.plugin.utils;

import org.apache.jmeter.control.ModuleController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import java.util.Map;
import java.util.Optional;

public class GetNodeData {

    public static String getNodeType(JMeterTreeNode node) {
        return node.getTestElement().getClass().getSimpleName();
    }

    public static void getNodeData(String prefix, JMeterTreeNode node, Map<String, String> map) {
        TestElement nodeTestElement = node.getTestElement();

        switch (nodeTestElement.getClass().getSimpleName()) {
            case "HTTPSamplerProxy":
                getHttpSamplerData(prefix, (HTTPSamplerProxy) nodeTestElement, map);
                break;
            case "TransactionController":
                getTransactionControllerData(prefix, (TransactionController) nodeTestElement, map);
                break;
            case "ModuleController":
                getModuleControllerDate(prefix, (ModuleController) nodeTestElement, map);
                break;
            default:
        }
    }

    public static void getHttpSamplerData(String prefix, HTTPSamplerProxy httpSampler, Map<String, String> map) {
        map.put(prefix + "name", httpSampler.getName());
        map.put(prefix + "comment", httpSampler.getComment());
        map.put(prefix + "protocol", httpSampler.getProtocol());
        map.put(prefix + "host", httpSampler.getDomain());
        map.put(prefix + "path", httpSampler.getPath().replaceAll("\\$\\{", "{"));
        map.put(prefix + "method", httpSampler.getMethod());
    }

    public static void getTransactionControllerData(String prefix, TransactionController transactionController, Map<String, String> map) {
        map.put(prefix + "name", transactionController.getName());
        map.put(prefix + "comment", transactionController.getComment());
        map.put(prefix + "isGenerate", String.valueOf(transactionController.isGenerateParentSample()));
        map.put(prefix + "isInclude", String.valueOf(transactionController.isIncludeTimers()));
    }

    public static void getModuleControllerDate(String prefix, ModuleController moduleController, Map<String, String> map) {
        map.put(prefix + "name", moduleController.getName());
        map.put(prefix + "comment", moduleController.getComment());
        JMeterTreeNode selectedNode = moduleController.getSelectedNode();
        map.put(prefix + "selectedElementName", (selectedNode == null) ? "" : selectedNode.getName());
    }
}
