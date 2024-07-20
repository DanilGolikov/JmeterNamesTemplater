package com.example.jmeter.plugin.utils;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.control.*;
import org.apache.jmeter.extractor.BoundaryExtractor;
import org.apache.jmeter.extractor.RegexExtractor;
import org.apache.jmeter.extractor.json.jmespath.JMESPathExtractor;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.sampler.DebugSampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class GetNodeData {

    private static final Logger log = LoggerFactory.getLogger(GetNodeData.class);

    public static String getNodeType(JMeterTreeNode node) {
        return node.getTestElement().getClass().getSimpleName();
    }

    public static void getNodeData(String prefix, JMeterTreeNode node, Map<String, String> map) {
        TestElement nodeTestElement = node.getTestElement();
        String methodName = "get" + nodeTestElement.getClass().getSimpleName() + "Data";

        try {
            map.put(prefix + "name", nodeTestElement.getName());
            map.put(prefix + "comment", nodeTestElement.getComment());
            Method method = GetNodeData.class.getDeclaredMethod(methodName, String.class, nodeTestElement.getClass(), Map.class);
            method.invoke(null, prefix, nodeTestElement, map);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//            log.warn("{} - {}\n{}", e.getMessage(), methodName, e.getStackTrace());
        }
    }



    /*
    ##########################
    #       SAMPLERS         #
    ##########################
     */
    public static void getHTTPSamplerProxyData(String prefix, HTTPSamplerProxy httpSampler, Map<String, String> map) {
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

    public static void getDebugSamplerData(String prefix, DebugSampler debugSampler, Map<String, String> map) {
        map.put(prefix + "jmeterProperties", String.valueOf(debugSampler.isDisplayJMeterProperties()));
        map.put(prefix + "jmeterVariables", String.valueOf(debugSampler.isDisplayJMeterVariables()));
        map.put(prefix + "systemProperties", String.valueOf(debugSampler.isDisplaySystemProperties()));
    }



    /*
    ##########################
    #   LOGIC CONTROLLERS    #
    ##########################
     */
    public static void getTransactionControllerData(String prefix, TransactionController transactionController, Map<String, String> map) {
        map.put(prefix + "isGenerate", String.valueOf(transactionController.isGenerateParentSample()));
        map.put(prefix + "isInclude", String.valueOf(transactionController.isIncludeTimers()));
    }

    public static void getModuleControllerData(String prefix, ModuleController moduleController, Map<String, String> map) {
        JMeterTreeNode selectedNode = moduleController.getSelectedNode();
        map.put(prefix + "selectedElementName", (selectedNode == null) ? "" : selectedNode.getName());
    }

    public static void getIfControllerData(String prefix, IfController ifController, Map<String, String> map) {
        map.put(prefix + "condition", ifController.getCondition());
        map.put(prefix + "interpretCondition", String.valueOf(ifController.isUseExpression()));
        map.put(prefix + "evaluateForAllChildren", String.valueOf(ifController.isEvaluateAll()));
    }

    public static void getLoopControllerData(String prefix, LoopController loopController, Map<String, String> map) {
        map.put(prefix + "loopString", loopController.getLoopString());
        map.put(prefix + "loopCount", String.valueOf(loopController.getLoops()));
    }

    public static void getWhileControllerData(String prefix, WhileController whileController, Map<String, String> map) {
        map.put(prefix + "condition", whileController.getCondition());
    }

    public static void getIncludeControllerData(String prefix, IncludeController includeController, Map<String, String> map) {
        String fullFilename = includeController.getIncludePath();
        map.put(prefix + "fullFilename", fullFilename);
        try {
            String[] splitFilename = fullFilename.split("[/\\\\]");
            map.put(prefix + "filename",splitFilename[splitFilename.length - 1]);
        } catch (Exception ignored) {
            map.put(prefix + "filename", "");
        }
    }

    public static void getRunTimeData(String prefix, RunTime runTime, Map<String, String> map) {
        map.put(prefix + "runtime", runTime.getRuntimeString());
    }

    public static void getThroughputControllerData(String prefix, ThroughputController throughputController, Map<String, String> map) {
        map.put(prefix + "basedOn", (throughputController.getStyle() == ThroughputController.BYNUMBER) ? "Total executions" : "Percent executions");
        map.put(prefix + "throughput", throughputController.getPercentThroughput());
        map.put(prefix + "perUser", String.valueOf(!throughputController.isPerThread()));
    }

    public static void getSwitchControllerData(String prefix, SwitchController switchController, Map<String, String> map) {
        map.put(prefix + "switchValue", switchController.getSelection());
    }



    /*
    ##########################
    #    POST PROCESSORS     #
    ##########################
     */
    public static void getJSONPostProcessorData(String prefix, JSONPostProcessor jsonPostProcessor, Map<String, String> map) {
        map.put(prefix + "name", jsonPostProcessor.getName());
        map.put(prefix + "comment", jsonPostProcessor.getComment());
        map.put(prefix + "varName", jsonPostProcessor.getVariableName());
        map.put(prefix + "jsonPath", jsonPostProcessor.getJsonPathExpressions());
        map.put(prefix + "matchNumber", jsonPostProcessor.getMatchNumbers());
        map.put(prefix + "defaultValue", jsonPostProcessor.getDefaultValues());
    }

    public static void getJMESPathExtractorData(String prefix, JMESPathExtractor jmesPathExtractor, Map<String, String> map) {
        map.put(prefix + "name", jmesPathExtractor.getName());
        map.put(prefix + "comment", jmesPathExtractor.getComment());
        map.put(prefix + "varName", jmesPathExtractor.getVariableName());
        map.put(prefix + "jmesPath", jmesPathExtractor.getJmesPathExpression());
        map.put(prefix + "matchNumber", jmesPathExtractor.getMatchNumber());
        map.put(prefix + "defaultValue", jmesPathExtractor.getDefaultValue());
    }

    public static void getBoundaryExtractorData(String prefix, BoundaryExtractor boundaryExtractor, Map<String, String> map) {
        map.put(prefix + "name", boundaryExtractor.getName());
        map.put(prefix + "comment", boundaryExtractor.getComment());
        map.put(prefix + "varName", boundaryExtractor.getVariableName());
        map.put(prefix + "leftBoundary", boundaryExtractor.getLeftBoundary());
        map.put(prefix + "rightBoundary", boundaryExtractor.getRightBoundary());
        map.put(prefix + "matchNumber", boundaryExtractor.getMatchNumberAsString());
        map.put(prefix + "defaultValue", boundaryExtractor.getDefaultValue());
    }

    public static void getRegexExtractorData(String prefix, RegexExtractor regexExtractor, Map<String, String> map) {
        map.put(prefix + "name", regexExtractor.getName());
        map.put(prefix + "comment", regexExtractor.getComment());
        map.put(prefix + "varName", regexExtractor.getVariableName());
        map.put(prefix + "regex", regexExtractor.getRegex());
        map.put(prefix + "template", regexExtractor.getTemplate());
        map.put(prefix + "matchNumber", regexExtractor.getMatchNumberAsString());
        map.put(prefix + "defaultValue", regexExtractor.getDefaultValue());
    }
}
