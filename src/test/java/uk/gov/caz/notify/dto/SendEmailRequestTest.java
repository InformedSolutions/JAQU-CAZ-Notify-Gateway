package uk.gov.caz.notify.dto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import uk.gov.caz.notify.dto.SendEmailRequest;

public class SendEmailRequestTest {

  @Test
  void canConstructSendEmailRequestEmptyConstructor() {
    SendEmailRequest sendEmailRequest = new SendEmailRequest();
    assertNotNull(sendEmailRequest);
  }

}
