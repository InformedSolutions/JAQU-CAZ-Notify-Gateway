package uk.gov.caz.notify.messaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.caz.notify.domain.QueueName;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;

/**
 * A client to interface with remote queue service.
 */
@Component
@Slf4j
public class MessagingClient {

  private final QueueMessagingTemplate messagingTemplate;

  private final GovUkNotifyWrapper govUkNotifyWrapper;

  @Value("${job.notify-gateway.new-queue-url}")
  String newQueue;

  @Value("${job.notify-gateway.dlq-url}")
  String deadLetterQueue;

  @Value("${job.notify-gateway.request-limit-queue-url}")
  String requestLimitQueue;

  @Value("${job.notify-gateway.service-down-queue-url}")
  String serviceDownQueue;

  @Value("${job.notify-gateway.service-error-queue-url}")
  String serviceErrorQueue;

  public MessagingClient(GovUkNotifyWrapper govUkNotifyWrapper,
      QueueMessagingTemplate messagingTemplate) {
    this.govUkNotifyWrapper = govUkNotifyWrapper;
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * A method to publish a message to a queue.
   * 
   * @param queueName the queue which the message should be sent to
   * @param message   the message object
   * @param headers   attributes that should be sent with the message
   */
  public void publishMessage(String queueName, Object message,
      Map<String, Object> headers) {
    messagingTemplate.convertAndSend(queueName, message, headers);
  }

  /**
   * Helper method to strip out useful headers and add new headers should a
   * message need to be sent to a new queue.
   * 
   * @param  oldHeaders the previous headers to be filtered
   * @return            new headers
   */
  public Map<String, Object> filterHeaders(Map<String, String> oldHeaders) {
    Map<String, Object> newHeaders = new HashMap<String, Object>();

    String messageGroupId = oldHeaders.get("MessageGroupId");
    newHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER, messageGroupId);
    newHeaders.put(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER,
        UUID.randomUUID().toString());

    return newHeaders;
  }

  /**
   * Helper method to retry sending the message.
   * 
   * @param  sendEmailRequest       the key parameters of the email to be sent
   * @param  i                      the number of attempts to try
   * @return                        true if message successfully sent, false
   *                                otherwise
   * @throws InstantiationException thrown if the API key for Notify is not set
   */
  private boolean retryMessage(SendEmailRequest sendEmailRequest, int i)
      throws InstantiationException {
    while (i > 0) {
      log.debug("Retrying message with reference: {}",
          sendEmailRequest.reference);
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

  /**
   * Handles the sending of the email and manages any errors thrown as a result
   * of this process.
   * 
   * @param  sendEmailRequest       the body of the email request to be sent
   * @param  newHeaders             the message headers received from SQS
   * @throws InstantiationException thrown if the API key for Notify is not set
   */
  public void handleMessage(SendEmailRequest sendEmailRequest,
      Map<String, Object> newHeaders) throws InstantiationException {
    try {
      log.info("Sending email with reference {}", sendEmailRequest.reference);
      govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId,
          sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
          sendEmailRequest.reference);
      log.debug("Message successfully sent: {}", sendEmailRequest.reference);
    } catch (NotificationClientException e) {
      int status = e.getHttpResult();
      log.error(e.getMessage());

      boolean success;

      switch (status) {
        case 400:
          publishMessage(deadLetterQueue, sendEmailRequest, newHeaders);
          break;
        case 403:
          publishMessage(deadLetterQueue, sendEmailRequest, newHeaders);
          break;
        case 429:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(requestLimitQueue, sendEmailRequest, newHeaders);
          }
          break;
        case 500:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceErrorQueue, sendEmailRequest, newHeaders);
          }
          break;
        case 503:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceDownQueue, sendEmailRequest, newHeaders);
          }
          break;
        default:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(deadLetterQueue, sendEmailRequest, newHeaders);
          }
          break;
      }
    } catch (IOException e) {
      log.error(e.getMessage());
      publishMessage(deadLetterQueue, sendEmailRequest, newHeaders);
    }
  }

  /**
   * Helper method to return a full queue name (with consideration for
   * environment) from the generic name of a queue.
   * 
   * @param  queueName generic name of the queue
   * @return           full name of queue target
   */
  public String getEnvQueueName(String queueName) {
    if (queueName.equals(QueueName.NEW.toString())) {
      return this.newQueue;
    } else if (queueName.equals(QueueName.REQUEST_LIMIT.toString())) {
      return this.requestLimitQueue;
    } else if (queueName.equals(QueueName.SERVICE_ERROR.toString())) {
      return this.serviceErrorQueue;
    } else if (queueName.equals(QueueName.SERVICE_DOWN.toString())) {
      return this.serviceDownQueue;
    } else {
      throw new IllegalArgumentException("Queue name not recognised.");
    }
  }
}
