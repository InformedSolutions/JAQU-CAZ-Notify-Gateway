package uk.gov.caz.notify.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  private final AmazonSQS client;
  private final GovUkNotifyWrapper govUkNotifyWrapper;
  private final ObjectMapper objectMapper;

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

  /**
   * Constructor for the external queuing provider client.
   * 
   * @param client a synchronous client for interfacing with Amazon SQS
   * @param govUkNotifyWrapper a wrapper for the Gov.UK Notify service
   * @param objectMapper library class for serializing and deserializing JSON
   */
  public MessagingClient(AmazonSQS client, GovUkNotifyWrapper govUkNotifyWrapper,
      ObjectMapper objectMapper) {
    this.client = client;
    this.govUkNotifyWrapper = govUkNotifyWrapper;
    this.objectMapper = objectMapper;
  }

  /**
   * A method to publish a message to a queue.
   * 
   * @param queueName the queue which the message should be sent to
   * @param message the message object
   */
  public void publishMessage(String queueName, String message) {
    SendMessageRequest sendMessageRequest = new SendMessageRequest();
    UUID messageDeduplicationId = UUID.randomUUID();

    sendMessageRequest.setMessageGroupId(UUID.randomUUID().toString());
    sendMessageRequest.setMessageDeduplicationId(messageDeduplicationId.toString());
    sendMessageRequest.putCustomRequestHeader("contentType", "application/json");
    sendMessageRequest.setQueueUrl(client.getQueueUrl(queueName).getQueueUrl());

    try {
      sendMessageRequest.setMessageBody(objectMapper.writeValueAsString(message));
    } catch (JsonProcessingException e) {
      log.error(e.getMessage());
    }

    log.info("Sending email message object to SQS queue {} with de-duplication ID: {}", queueName,
        messageDeduplicationId);
    client.sendMessage(sendMessageRequest);
  }

  /**
   * Helper method to strip out useful headers and add new headers should a message need to be sent
   * to a new queue.
   * 
   * @param oldHeaders the previous headers to be filtered
   * @return new headers
   */
  public Map<String, Object> filterHeaders(Map<String, String> oldHeaders) {
    Map<String, Object> newHeaders = new HashMap<String, Object>();

    String messageGroupId = oldHeaders.get("MessageGroupId");
    newHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER, messageGroupId);
    newHeaders.put(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());

    return newHeaders;
  }

  /**
   * Helper method to retry sending the message.
   * 
   * @param sendEmailRequest the key parameters of the email to be sent
   * @param i the number of attempts to try
   * @return true if message successfully sent, false otherwise
   * @throws InstantiationException thrown if the API key for Notify is not set
   */
  private boolean retryMessage(SendEmailRequest sendEmailRequest, int i)
      throws InstantiationException {
    while (i > 0) {
      log.info("Retrying message with reference: {}", sendEmailRequest.reference);
      try {
        govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId, sendEmailRequest.emailAddress,
            sendEmailRequest.personalisation, sendEmailRequest.reference);
        return true;
      } catch (NotificationClientException | IOException e) {
        i = i - 1;
      }
    }

    return false;
  }

  /**
   * Handles the sending of the email and manages any errors thrown as a result of this process.
   * 
   * @param sendEmailRequest the body of the email request to be sent
   * @throws JsonProcessingException thrown if the request cannot be written to a string
   * @throws InstantiationException thrown if the API key for Notify is not set
   */
  public void handleMessage(SendEmailRequest sendEmailRequest)
      throws JsonProcessingException, InstantiationException {
    String msgBody = objectMapper.writeValueAsString(sendEmailRequest);
    try {
      log.info("Sending email with reference {}", sendEmailRequest.reference);
      govUkNotifyWrapper.sendEmail(sendEmailRequest.templateId, sendEmailRequest.emailAddress,
          sendEmailRequest.personalisation, sendEmailRequest.reference);
      log.info("Message successfully sent: {}", sendEmailRequest.reference);
    } catch (NotificationClientException e) {
      int status = e.getHttpResult();
      log.error(e.getMessage());
      log.info("Got status {} from Notify Gateway for message: {}", status,
          sendEmailRequest.reference);

      boolean success;

      switch (status) {
        case 400:
          log.info("Publishing message with reference {} to the dead letter queue",
              sendEmailRequest.reference);
          publishMessage(deadLetterQueue, msgBody);
          break;
        case 403:
          log.info("Publishing message with reference {} to the dead letter queue",
              sendEmailRequest.reference);
          publishMessage(deadLetterQueue, msgBody);
          break;
        case 429:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.info("Message successfully sent: {}", sendEmailRequest.reference);
          } else {
            log.info("Publishing message with reference {} to the request limit queue",
                sendEmailRequest.reference);
            publishMessage(requestLimitQueue, msgBody);
          }
          break;
        case 500:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.info("Message successfully sent: {}", sendEmailRequest.reference);
          } else {
            log.info("Publishing message with reference {} to the service error queue",
                sendEmailRequest.reference);
            publishMessage(serviceErrorQueue, msgBody);
          }
          break;
        case 503:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.info("Message successfully sent: {}", sendEmailRequest.reference);
          } else {
            log.info("Publishing message with reference {} to the service down queue",
                sendEmailRequest.reference);
            publishMessage(serviceDownQueue, msgBody);
          }
          break;
        default:
          success = retryMessage(sendEmailRequest, 3);
          if (success) {
            log.info("Message successfully sent: {}", sendEmailRequest.reference);
          } else {
            log.info("Publishing message with reference {} to the dead letter queue",
                sendEmailRequest.reference);
            publishMessage(deadLetterQueue, msgBody);
          }
          break;
      }
    } catch (IOException e) {
      log.error(e.getMessage());
      publishMessage(deadLetterQueue, msgBody);
    }
  }

  /**
   * Helper method to return a full queue name (with consideration for environment) from the generic
   * name of a queue.
   * 
   * @param queueName generic name of the queue
   * @return full name of queue target
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
