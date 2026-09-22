package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.SentimentType;
import org.springframework.stereotype.Service;

@Service
public class SentimentAnalysisService {

    public SentimentResult analyze(String message)
    {
        if(message==null || message.isEmpty())
        {
            throw  new IllegalArgumentException("message is null or empty");
        }
        double confidence = 1.;
        SentimentType SentimentResult = SentimentType.NEUTRAL;
        SentimentResult result = new SentimentResult(SentimentResult,confidence);
        return  result;
    }

}
