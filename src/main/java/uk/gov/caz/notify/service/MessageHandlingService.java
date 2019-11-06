package uk.gov.caz.notify.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.messaging.MessagingClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHandlingService {

  private final AmazonSQS amazonSqs;

  private final MessagingClient messagingClient;

  @Value("${application.message-batch-rate}")
  private int messageBatchRate;

  @Value("${job.notify-gateway.dlq-url}")
  private String dlqName;

  /**
   * Fetch messages from a queue and process each message. Send the message to
   * the DLQ if the message body can't be deserialized or if the MessageGroupId
   * header is missing.
   * 
   * @param queueName the name of the queue to receive messages from
   */
  public void sendQueuedMessages(String queueName) {

    ObjectMapper objectMapper = new ObjectMapper();
    List<Message> messageList = this.getQueueMessageByQueueUrl(queueName);

    for (Message message : messageList) {
      Map<String, Object> newHeaders =
          messagingClient.filterHeaders(message.getAttributes());
      log.info("Received message: {}", message.toString());
      try {
        SendEmailRequest request =
            objectMapper.readValue(message.getBody(), SendEmailRequest.class);
        messagingClient.handleMessage(request, newHeaders);
      } catch (IOException | NoSuchElementException
          | InstantiationException err) {
        log.error("Failed to process message with id: {}",
            message.getMessageId());
        messagingClient.publishMessage(dlqName, message.getBody(), newHeaders);
        continue;
      }
    }
  }

  private List<Message> getQueueMessageByQueueUrl(String queueName) {
    log.info("Getting messages from queue url: {}", queueName);

    GetQueueUrlResult getQueueUrlResult = amazonSqs.getQueueUrl(queueName);
    String queueUrl = getQueueUrlResult.getQueueUrl();

    ReceiveMessageRequest messageRequest = new ReceiveMessageRequest(queueUrl)
        .withWaitTimeSeconds(5).withMaxNumberOfMessages(messageBatchRate);

    List<Message> messages =
        amazonSqs.receiveMessage(messageRequest).getMessages();

    log.info("Received total messages size: {}", messages.size());

    return messages;
  }

}
