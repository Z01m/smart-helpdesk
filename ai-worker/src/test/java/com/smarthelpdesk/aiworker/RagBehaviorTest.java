package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.rag.*;
import com.smarthelpdesk.aiworker.config.RagProperties;
import com.smarthelpdesk.aiworker.client.KnowledgeBaseClient;
import com.smarthelpdesk.aiworker.dto.knowledge.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RagBehaviorTest {
 final EmbeddingService embeddings=mock(EmbeddingService.class);
 final KnowledgeBaseClient knowledge=mock(KnowledgeBaseClient.class);
 final RagProperties props=new RagProperties();
 final RagPipeline rag=new RagPipeline(embeddings,knowledge,props,new ContextBuilder());
 ScoredChunk chunk(String text,double score){return new ScoredChunk(UUID.randomUUID(),"Title",text,score);}
 @Test void retrievesEmbeddingWithRequestedLimit(){
  when(embeddings.embed("query")).thenReturn(new float[]{1,0});
  when(knowledge.search(any())).thenReturn(new KnowledgeSearchResponse(List.of(chunk("context",0.9))));
  assertThat(rag.retrieveContext("query",3)).hasSize(1);
  verify(knowledge).search(argThat(q->q.topK()==3 && Arrays.equals(q.embedding(),new float[]{1,0})));
 }
 @Test void emptyResultsRequireHuman(){assertThat(rag.hasRelevantContext(List.of())).isFalse();assertThat(rag.getMaxScore(List.of())).isZero();}
 @Test void negativeCosineDoesNotBecomeNegativeConfidence(){assertThat(rag.getMaxScore(List.of(chunk("x",-0.8)))).isZero();}
 @Test void invalidScoresOrBlankTextCannotAuthorizeAutomaticAnswer(){
  assertThat(rag.hasRelevantContext(List.of(chunk("",1)))).isFalse();
  assertThat(rag.hasRelevantContext(List.of(chunk("x",Double.POSITIVE_INFINITY)))).isFalse();
 }
 @Test void preservesValidScoreAlongsideInvalidOne(){assertThat(rag.getMaxScore(List.of(chunk("x",Double.NaN),chunk("y",0.9)))).isEqualTo(0.9);}
 @Test void rejectsInvalidRequestBeforeNetwork(){assertThatThrownBy(()->rag.retrieveContext(" ",5)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->rag.retrieveContext("x",0)).isInstanceOf(IllegalArgumentException.class);verifyNoInteractions(knowledge,embeddings);}
 @Test void longContextIsTruncatedNotLost(){String text="long useful passage ".repeat(200);String context=new ContextBuilder().build(List.of(chunk(text,.9)),100);assertThat(context).contains("long useful");assertThat(context.length()).isLessThanOrEqualTo(400);}
 @Test void contextDeduplicatesPassages(){String context=new ContextBuilder().build(List.of(chunk("unique passage",.9),chunk("unique passage",.8)),100);assertThat(context.split("unique passage",-1)).hasSize(2);}
 @Test void tinyBudgetDoesNotOverflow(){assertThat(new ContextBuilder().build(List.of(chunk("text",.9)),1)).isEmpty();}
}
