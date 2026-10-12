package com.smarthelpdesk.aiworker;
import com.smarthelpdesk.aiworker.service.PriorityCalculationService;
import com.smarthelpdesk.aiworker.dto.ai.*;
import com.smarthelpdesk.aiworker.dto.ai.enums.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
class PriorityRulesTest {
 @ParameterizedTest @CsvSource({"STANDARD,SECURITY,NEUTRAL,CRITICAL","VIP,SECURITY,ANGRY,CRITICAL","VIP,PAYMENT,POSITIVE,HIGH","PREMIUM,PAYMENT,ANGRY,HIGH","STANDARD,PAYMENT,NEGATIVE,HIGH","STANDARD,PAYMENT,NEUTRAL,NORMAL","PREMIUM,PAYMENT,POSITIVE,NORMAL"})
 void appliesPriorityRules(CustomerTier tier,TicketCategory category,SentimentType sentiment,PriorityLevel expected){
 assertThat(new PriorityCalculationService().calculate(tier,new ClassificationResult(category,1),new SentimentResult(sentiment,0)).priority()).isEqualTo(expected);
 }
}
