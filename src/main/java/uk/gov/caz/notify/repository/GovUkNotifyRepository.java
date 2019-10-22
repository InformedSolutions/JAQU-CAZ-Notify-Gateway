package uk.gov.caz.notify.repository;

import java.io.IOException;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@Repository
public class GovUkNotifyRepository {

  private NotificationClient client;

  public GovUkNotifyRepository(
      @Value("${notify-api-key}") String notifyApiKey) {
    if (notifyApiKey != null && !(notifyApiKey.isEmpty())) {
      this.client = new NotificationClient(notifyApiKey);
    }

  }

  public SendEmailResponse sendEmail(String templateId, String emailAddress,
      String personalisation, String reference)
      throws NotificationClientException, JsonParseException,
      JsonMappingException, IOException {
    HashMap<String, Object> personalisationMap =
        new ObjectMapper().readValue(personalisation, HashMap.class);
    return client.sendEmail(templateId, emailAddress, personalisationMap,
        reference);
  }

}
