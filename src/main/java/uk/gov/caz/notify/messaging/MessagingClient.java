package uk.gov.caz.notify.messaging;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;
import uk.gov.caz.notify.dto.NotifyErrorResponse;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;

@Component
@Slf4j
@ConditionalOnProperty(value = "application.consume", havingValue = "true",
    matchIfMissing = false)
public class MessagingClient {

  private final QueueMessagingTemplate messagingTemplate;

  private final GovUkNotifyWrapper govUkNotifyWrapper;

  @Value("${application.queue.dlq}")
  String dlq;

  @Value("${application.queue.request-limit}")
  String requestLimit;

  @Value("${application.queue.service-down}")
  String serviceDown;

  @Value("${application.queue.service-error}")
  String serviceError;

  public MessagingClient(GovUkNotifyWrapper govUkNotifyWrapper,
      QueueMessagingTemplate messagingTemplate) {
    this.govUkNotifyWrapper = govUkNotifyWrapper;
    this.messagingTemplate = messagingTemplate;
  }

  private void publishMessage(String queueName, Object message) {
    messagingTemplate.convertAndSend(queueName, message);
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
      govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId,
          sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
          sendEmailRequest.reference);
      log.debug("Message successfully sent: {}", sendEmailRequest.reference);
    } catch (NotificationClientException e) {
      int status = e.getHttpResult();

      NotifyErrorResponse msg =
          new NotifyErrorResponse(status, e.getMessage(), sendEmailRequest);
      boolean success;

      switch (status) {
        case 400:
          publishMessage(dlq, msg);
          break;
        case 403:
          publishMessage(dlq, msg);
          break;
        case 429:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(requestLimit, msg);
          }
          break;
        case 500:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceError, msg);
          }
          break;
        case 503:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceDown, msg);
          }
          break;
        default:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(dlq, msg);
          }
          break;
      }
    } catch (IOException e) {
      NotifyErrorResponse msg =
          new NotifyErrorResponse(400, e.getMessage(), sendEmailRequest);
      // publishMessage(dlq, msg);
      log.info(msg.toString());
    }
  }

  /**
   * Helper method to retry sending the message.
   * 
   * @param  sendEmailRequest the key parameters of the email to be sent
   * @param  i                the number of attempts to try
   * @return                  true if message successfully sent, false otherwise
   */
  private boolean retryMessage(SendEmailRequest sendEmailRequest, int i) {
    while (i > 0) {
      try {
        govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId,
            sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
            sendEmailRequest.reference);
        return true;
      } catch (NotificationClientException | IOException e) {
        i = i - 1;
      }
    }

    return false;
  }
}
