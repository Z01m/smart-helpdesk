package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.service.*;
import com.smarthelpdesk.aiworker.ai.*;
import com.smarthelpdesk.aiworker.rag.RagPipeline;
import com.smarthelpdesk.aiworker.entity.TicketResult;
import com.smarthelpdesk.aiworker.dto.ai.*;
import com.smarthelpdesk.aiworker.dto.ai.enums.*;
import com.smarthelpdesk.aiworker.dto.knowledge.ScoredChunk;
import kafka.event.TicketCreatedEvent;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrchestratorTest {
 ClassificationService classify=mock(ClassificationService.class);SentimentAnalysisService sentiment=mock(SentimentAnalysisService.class);
 AnswerGenerationService answer=mock(AnswerGenerationService.class);TicketResultService results=mock(TicketResultService.class);RagPipeline rag=mock(RagPipeline.class);
 TicketProcessingOrchestrator service=new TicketProcessingOrchestrator(classify,sentiment,new PriorityCalculationService(),answer,results,rag,JsonMapper.builder().build());
 TicketCreatedEvent event=TicketCreatedEvent.builder().ticketId(UUID.randomUUID()).message("question").customerTier("VIP").build();
 @BeforeEach void setup(){when(classify.classify(any())).thenReturn(new ClassificationResult(TicketCategory.PAYMENT,.9));when(sentiment.analyze(any())).thenReturn(new SentimentResult(SentimentType.NEUTRAL,0));when(results.save(any())).thenAnswer(i->i.getArgument(0));}
 @Test void noContextUsesOperatorFallbackWithoutCallingAnswerModel(){when(rag.retrieveContext(anyString())).thenReturn(List.of());var result=service.process(event);assertThat(result.requiresHumanReview()).isTrue();assertThat(result.ticketResult().getGeneratedAnswer()).contains("оператора");assertThat(result.ticketResult().getPriority()).isEqualTo("HIGH");verifyNoInteractions(answer);}
 @Test void relevantContextProducesAnswerAndSourceMetadata(){
  UUID article=UUID.randomUUID();var chunks=List.of(new ScoredChunk(article,"Title","text",.9));when(rag.retrieveContext(anyString())).thenReturn(chunks);when(rag.hasRelevantContext(chunks)).thenReturn(true);when(rag.getMaxScore(chunks)).thenReturn(.9);when(rag.buildContext(chunks,1500)).thenReturn("context");
  when(answer.generate(any(),any(),any(),any(),any(),anyDouble())).thenReturn(new GeneratedAnswer("answer",List.of(),12));
  var result=service.process(event);assertThat(result.requiresHumanReview()).isFalse();assertThat(result.ticketResult().getGeneratedAnswer()).isEqualTo("answer");assertThat(result.ticketResult().getContextSources()).contains(article.toString());
 }
 @Test void dependencyFailureCannotSavePartialResult(){when(classify.classify(any())).thenThrow(new IllegalStateException("offline"));assertThatThrownBy(()->service.process(event)).isInstanceOf(IllegalStateException.class);verifyNoInteractions(results,answer,rag);}
 @Test void answerGenerationUsesUsageAndRejectsEmptyAnswer(){
  var ai=mock(AiClient.class);var builder=mock(PromptBuilder.class);var generator=new AnswerGenerationService(builder,ai);
  var c=new ClassificationResult(TicketCategory.PAYMENT,.9);var s=new SentimentResult(SentimentType.NEUTRAL,0);var p=new PriorityResult(PriorityLevel.NORMAL);
  when(ai.chatCompletion(any())).thenReturn(new AiResponse(" answer ",Map.of("promptTokens",10,"completionTokens",5)));
  assertThat(generator.generate("q",null,c,s,p,.9).tokensUsed()).isEqualTo(15);
  when(ai.chatCompletion(any())).thenReturn(new AiResponse(" ",Map.of()));assertThatThrownBy(()->generator.generate("q","",c,s,p,.9)).isInstanceOf(IllegalStateException.class);
  assertThatThrownBy(()->generator.generate("q","",c,s,p,Double.NaN)).isInstanceOf(IllegalArgumentException.class);
 }
}
