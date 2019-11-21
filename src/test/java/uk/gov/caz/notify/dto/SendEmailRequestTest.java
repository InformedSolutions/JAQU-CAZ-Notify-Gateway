package uk.gov.caz.notify.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import uk.gov.caz.notify.dto.SendEmailRequest;

public class SendEmailRequestTest {

  @Test
  void canConstructSendEmailRequestEmptyConstructor() {
    SendEmailRequest sendEmailRequest = new SendEmailRequest();
    assertNotNull(sendEmailRequest);
  }

  @Test
  void canConstructSendEmailRequestFullConstructor() {
    String template = "testTemplate";
    String email = "testEmail";
    String personalisation = "testPersonalisation";
    String reference = "testReference";
    SendEmailRequest sendEmailRequest =
        new SendEmailRequest(template, email, personalisation, reference);
    assertNotNull(sendEmailRequest);
    assertEquals(template, sendEmailRequest.templateId);
    assertEquals(email, sendEmailRequest.emailAddress);
    assertEquals(personalisation, sendEmailRequest.personalisation);
    assertEquals(reference, sendEmailRequest.reference);
  }

}
