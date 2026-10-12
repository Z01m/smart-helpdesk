package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.ai.AiResponseParser;
import com.smarthelpdesk.aiworker.dto.ai.enums.*;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;
class AiParsingTest {
 final AiResponseParser parser=new AiResponseParser(JsonMapper.builder().build());
 @ParameterizedTest @ValueSource(strings={"", " ", "not json", "null", "{}", "{\"category\":\"PAYMENT\"}", "{\"category\":\"UNKNOWN\",\"confidence\":0.5}", "{\"category\":\"PAYMENT\",\"confidence\":-0.1}", "{\"category\":\"PAYMENT\",\"confidence\":1.1}", "{\"category\":\"PAYMENT\",\"confidence\":\"NaN\"}"})
 void rejectsInvalidClassification(String json){assertThatThrownBy(()->parser.parseClassification(json)).isInstanceOf(IllegalArgumentException.class);}
 @ParameterizedTest @ValueSource(strings={"", "null", "{}", "{\"sentiment\":\"ANGRY\"}", "{\"sentiment\":\"NEUTRAL\",\"score\":2}", "{\"sentiment\":\"NEUTRAL\",\"score\":\"NaN\"}"})
 void rejectsInvalidSentiment(String json){assertThatThrownBy(()->parser.parseSentiment(json)).isInstanceOf(IllegalArgumentException.class);}
 @Test void normalizesCaseAndAcceptsBoundaryScores(){
  assertThat(parser.parseClassification("{\"category\":\" payment \",\"confidence\":1}").category()).isEqualTo(TicketCategory.PAYMENT);
  assertThat(parser.parseSentiment("{\"sentiment\":\" angry \",\"score\":-1}").sentiment()).isEqualTo(SentimentType.ANGRY);
 }
 @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\n"})
 void rejectsEmptyAnswer(String text){assertThatThrownBy(()->parser.parseAnswer(text)).isInstanceOf(IllegalArgumentException.class);}
 @Test void trimsAnswer(){assertThat(parser.parseAnswer(" answer ")).isEqualTo("answer");}
}
