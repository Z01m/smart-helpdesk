package com.smarthelpdesk.aiworker.servise;

import com.smarthelpdesk.aiworker.dto.ai.ClassificationResult;
import com.smarthelpdesk.aiworker.dto.ai.enums.TicketCategory;
import com.smarthelpdesk.aiworker.service.ClassificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ClassificationServiceOllamaIT {

    @Autowired
    private ClassificationService classificationService;

    @Test
    void shouldClassifyPaymentTicketUsingRealOllama() {

        String message = "My card was charged twice for the same payment.";

        ClassificationResult result =
                classificationService.classify(message);

        assertNotNull(result);

        assertNotNull(result.category());

        assertTrue(
                result.confidence() >= 0.0
                        && result.confidence() <= 1.0
        );

        assertEquals(
                TicketCategory.PAYMENT,
                result.category()
        );
    }
}
