package uk.gov.caz.notify.messaging;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyRepository;
import uk.gov.service.notify.NotificationClientException;

@Component
@Slf4j
@ConditionalOnProperty(value = "application.consume", havingValue = "true",
    matchIfMissing = false)
public class MessagingListener {

  private final GovUkNotifyRepository govUkNotifyRepository;

  public MessagingListener(GovUkNotifyRepository govUkNotifyRepository) {
    this.govUkNotifyRepository = govUkNotifyRepository;
  }

  /**
   * A listener method to consume messages from the 'new' message queue.
   * 
   * @param sendEmailRequest the email message to be sent
   */
  @SqsListener(value = "${application.queue.new}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeMessage(SendEmailRequest sendEmailRequest) {
    log.debug("Received message: {}", sendEmailRequest.reference);
    try {
      govUkNotifyRepository.sendEmail(sendEmailRequest.templateId,
          sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
          sendEmailRequest.reference);
      log.debug("Message successfully sent: {}", sendEmailRequest.reference);
    } catch (NotificationClientException e) {
      log.warn(e.getMessage());
    } catch (IOException e) {
      log.warn(e.getMessage());
    }
  }
}
