package com.smarthelpdesk.aiworker.ai;

import com.smarthelpdesk.aiworker.dto.ai.AiResponse;
import com.smarthelpdesk.aiworker.dto.ai.PromptRequest;

public interface AiClient {

    AiResponse chatCompletion(PromptRequest request);

    float[] embed(String text);
}