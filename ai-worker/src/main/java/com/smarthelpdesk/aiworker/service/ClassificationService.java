package com.smarthelpdesk.aiworker.service;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ClassificationService {

    public ClassificationResult classify(String message)
    {
        if(message==null || message.isEmpty())
        {
            throw  new IllegalArgumentException("message is null or empty");
        }
        double confidence = 1.;
        ClassificationResult result = new ClassificationResult(TicketCategory.GENERAL,confidence);
        return  result;
    }





}
