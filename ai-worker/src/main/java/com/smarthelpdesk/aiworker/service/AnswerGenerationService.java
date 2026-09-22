package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.GeneratedAnswer;
import com.smarthelpdesk.aiworker.dto.ai.PriorityResult;
import com.smarthelpdesk.aiworker.dto.ai.SentimentResult;
import org.springframework.stereotype.Service;

@Service
public class AnswerGenerationService {


    public GeneratedAnswer generate(String message, ClassificationResult classificationResult, SentimentResult sentimentResult, PriorityResult priorityResult) {
        if(message==null || message.isEmpty()){
            throw new IllegalArgumentException("message cannot be null or empty");
        }
        if(classificationResult==null){
            throw new IllegalArgumentException("classificationResult cannot be null or empty");
        }
        if(sentimentResult==null){
            throw new IllegalArgumentException("sentimentResult cannot be null or empty");
        }
        if(priorityResult==null){
            throw new IllegalArgumentException("priorityResult cannot be null or empty");
        }
        GeneratedAnswer answer = new GeneratedAnswer("test",1);
        return answer;
    }
}
