package com.smarthelpdesk.aiworker.rag;

import com.smarthelpdesk.aiworker.ai.AiClient;
import com.smarthelpdesk.aiworker.ai.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    public final AiClient aiClient;

    public float[] embed(String text){
        if(text == null||text.isEmpty()){
            throw new IllegalArgumentException("text is null or empty");
        }
        float[] result = aiClient.embed(text);
        if(result == null||result.length==0){
            throw new IllegalStateException("result is null or empty");
        }
        return result;
    }

    public List<float[]> embedBatch(List<String> texts){
        if(texts==null||texts.isEmpty()){
            throw new IllegalArgumentException("texts is null or empty");
        }
        List<float[]> result = new ArrayList<>();
        for(String text:texts){
            result.add(embed(text));
        }
        return result;
    }

}
