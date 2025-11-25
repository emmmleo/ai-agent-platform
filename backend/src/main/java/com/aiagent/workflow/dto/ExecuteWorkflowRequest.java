package com.aiagent.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 执行工作流请求
 */
public class ExecuteWorkflowRequest {

    @JsonProperty("inputParams")
    private Map<String, Object> inputParams;

    public Map<String, Object> getInputParams() {
        return inputParams;
    }

    public void setInputParams(Map<String, Object> inputParams) {
        this.inputParams = inputParams;
    }
}

