package com.example.jmeter.plugin.utils;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.control.ModuleController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GetNodeData {

//    private static final Logger log = LoggerFactory.getLogger(GetNodeData.class);

    public static String getNodeType(JMeterTreeNode node) {
        return node.getTestElement().getClass().getSimpleName();
    }

    public static void getNodeData(String prefix, JMeterTreeNode node, Map<String, String> map) {
        TestElement nodeTestElement = node.getTestElement();
        String methodName = "get" + nodeTestElement.getClass().getSimpleName() + "Data";

        try {
            Method method = GetNodeData.class.getDeclaredMethod(methodName, String.class, nodeTestElement.getClass(), Map.class);
            method.invoke(null, prefix, nodeTestElement, map);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//            log.warn("{} - {}\n{}", e.getMessage(), methodName, e.getStackTrace());
        }

//        switch (nodeTestElement.getClass().getSimpleName()) {
//            case "HTTPSamplerProxy":
//                getHttpSamplerData(prefix, (HTTPSamplerProxy) nodeTestElement, map);
//                break;
//            case "TransactionController":
//                getTransactionControllerData(prefix, (TransactionController) nodeTestElement, map);
//                break;
//            case "ModuleController":
//                getModuleControllerDate(prefix, (ModuleController) nodeTestElement, map);
//                break;
//            default:
//        }
    }

    public static void getHTTPSamplerProxyData(String prefix, HTTPSamplerProxy httpSampler, Map<String, String> map) {
        map.put(prefix + "name", httpSampler.getName());
        map.put(prefix + "comment", httpSampler.getComment());
        map.put(prefix + "protocol", httpSampler.getProtocol());
        map.put(prefix + "host", httpSampler.getDomain());
        map.put(prefix + "path", httpSampler.getPath().replaceAll("\\$\\{", "{"));
        map.put(prefix + "method", httpSampler.getMethod());

        StringBuilder params = new StringBuilder();
        int paramIndex = 1;
        CollectionProperty args = httpSampler.getArguments().getArguments();
        for (JMeterProperty jMeterProperty : args) {
            Argument argument = (Argument) jMeterProperty.getObjectValue();
            params.append(String.format("%s=%s&", argument.getName(), argument.getValue()));
            map.put(prefix + "param." + paramIndex++, argument.getValue());
        }
        if (params.length() > 0)
            params.setLength(params.length() - 1);
        map.put(prefix + "params", params.toString());

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
