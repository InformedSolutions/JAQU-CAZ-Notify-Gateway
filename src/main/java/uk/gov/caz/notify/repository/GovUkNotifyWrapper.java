package uk.gov.caz.notify.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@Component
public class GovUkNotifyWrapper {

  private NotificationClient client;

  /**
   * GovUkNotifyWrapper constructor.
   * 
   * @param notifyApiKey as retrieved from AWS secrets
   */
  public GovUkNotifyWrapper(
      @Value("${notify-api-key-team}") String notifyApiKey) {
    if (notifyApiKey != null && !(notifyApiKey.isEmpty())) {
      this.client = new NotificationClient(notifyApiKey);
    }

  }

  /**
   * Uses the Gov.UK Notify API to send an email
   * 
   * @param  templateId                  the identifier of the email template
   * @param  emailAddress                the recipient
   * @param  personalisation             parameters needed for the email
   * @param  reference                   an unique identifier
   * @return                             the response returned from Gov.UK
   *                                     Notify
   * @throws NotificationClientException any error thrown by Gov.UK Notify
   * @throws IOException                 thrown if the personalisation JSON is
   *                                       incorrectly formed
   */
  public SendEmailResponse sendEmail(String templateId, String emailAddress,
      String personalisation, String reference)
      throws NotificationClientException, IOException {
    HashMap<String, Object> personalisationMap =
        new ObjectMapper().readValue(personalisation, HashMap.class);
    return client.sendEmail(templateId, emailAddress, personalisationMap,
        reference);
  }

}
