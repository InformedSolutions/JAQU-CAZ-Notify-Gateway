package uk.gov.caz.notify.messaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;

/**
 * A client to interface with remote queue service.
 */
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

  private void publishMessage(String queueName, Object message,
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
  private Map<String, Object> filterHeaders(Map<String, Object> oldHeaders) {
    Map<String, Object> newHeaders = new HashMap<String, Object>();

    newHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER,
        oldHeaders.get("MessageGroupId"));
    newHeaders.put(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER,
        UUID.randomUUID().toString());

    return newHeaders;
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

  /**
   * Handles the sending of the email and manages any errors thrown as a result
   * of this process.
   * 
   * @param sendEmailRequest the body of the email request to be sent
   * @param headers          the message headers received from SQS
   */
  public void handleMessage(SendEmailRequest sendEmailRequest,
      Map<String, Object> headers) {
    try {
      govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId,
          sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
          sendEmailRequest.reference);
      log.debug("Message successfully sent: {}", sendEmailRequest.reference);
    } catch (NotificationClientException e) {
      int status = e.getHttpResult();
      log.error(e.getMessage());

      Map<String, Object> newHeaders = filterHeaders(headers);

      boolean success;

      switch (status) {
        case 400:
          publishMessage(dlq, sendEmailRequest, newHeaders);
          break;
        case 403:
          publishMessage(dlq, sendEmailRequest, newHeaders);
          break;
        case 429:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(requestLimit, sendEmailRequest, newHeaders);
          }
          break;
        case 500:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceError, sendEmailRequest, newHeaders);
          }
          break;
        case 503:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(serviceDown, sendEmailRequest, newHeaders);
          }
          break;
        default:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.debug("Message successfully sent: {}",
                sendEmailRequest.reference);
          } else {
            publishMessage(dlq, sendEmailRequest, newHeaders);
          }
          break;
      }
    } catch (IOException e) {
      log.error(e.getMessage());
      Map<String, Object> newHeaders = filterHeaders(headers);
      publishMessage(dlq, sendEmailRequest, newHeaders);
    }
  }

  /**
   * A listener method to consume messages from the 'new' message queue.
   * 
   * @param sendEmailRequest the email message to be sent
   */
  @SqsListener(value = "${application.queue.new}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeNewMessage(SendEmailRequest sendEmailRequest,
      @Headers Map<String, Object> headers) {
    log.debug("Received message from new queue: {}",
        sendEmailRequest.reference);
    handleMessage(sendEmailRequest, headers);
  }

  /**
   * A listener method to consume messages from the 'request limit' message
   * queue.
   * 
   * @param sendEmailRequest the email message to be sent
   */
  @SqsListener(value = "${application.queue.request-limit}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeRequestLimitMessage(SendEmailRequest sendEmailRequest,
      @Headers Map<String, Object> headers) {
    log.debug("Received message from request limit queue: {}",
        sendEmailRequest.reference);
    handleMessage(sendEmailRequest, headers);
  }

  /**
   * A listener method to consume messages from the 'service down' message
   * queue.
   * 
   * @param sendEmailRequest the email message to be sent
   */
  @SqsListener(value = "${application.queue.service-down}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeServiceDownMessage(SendEmailRequest sendEmailRequest,
      @Headers Map<String, Object> headers) {
    log.debug("Received message from service down queue: {}",
        sendEmailRequest.reference);
    handleMessage(sendEmailRequest, headers);
  }


  /**
   * A listener method to consume messages from the 'service error' message
   * queue.
   * 
   * @param sendEmailRequest the email message to be sent
   */
  @SqsListener(value = "${application.queue.service-error}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeServiceErrorMessage(SendEmailRequest sendEmailRequest,
      @Headers Map<String, Object> headers) {
    log.debug("Received message from service error queue: {}",
        sendEmailRequest.reference);
    handleMessage(sendEmailRequest, headers);
  }
}
