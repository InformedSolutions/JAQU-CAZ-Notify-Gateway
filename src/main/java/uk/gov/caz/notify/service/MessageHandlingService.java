package uk.gov.caz.notify.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
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

  private String queueUrl;

  /**
   * Fetch messages from a queue and process each message. Send the message to the DLQ if the
   * message body can't be deserialized or if the MessageGroupId header is missing.
   * 
   * @param queueName the name of the queue to receive messages from
   */
  public void sendQueuedMessages(String queueName) {

    ObjectMapper objectMapper = new ObjectMapper();
    List<Message> messageList = this.getQueueMessageByQueueUrl(queueName);

    for (Message message : messageList) {
      log.info("Processing message with ID: {}", message.getMessageId());

      try {
        SendEmailRequest request =
            objectMapper.readValue(message.getBody(), SendEmailRequest.class);
        messagingClient.handleMessage(request);
      } catch (IOException | InstantiationException e) {
        log.error("Failed to process message with id: {}", message.getMessageId());
        SendMessageRequest request = messagingClient.createSendMessageRequest(message.getBody());
        messagingClient.publishMessage(dlqName, request);
      }
      amazonSqs.deleteMessage(this.queueUrl, message.getReceiptHandle());
    }
  }

  private List<Message> getQueueMessageByQueueUrl(String queueName) {

    String envQueueName = messagingClient.getEnvQueueName(queueName);
    log.info("Getting messages from queue url: {}", envQueueName);

    GetQueueUrlResult getQueueUrlResult = amazonSqs.getQueueUrl(envQueueName);
    this.queueUrl = getQueueUrlResult.getQueueUrl();

    ReceiveMessageRequest messageRequest =
        new ReceiveMessageRequest(this.queueUrl).withWaitTimeSeconds(5)
            .withMaxNumberOfMessages(messageBatchRate).withAttributeNames("MessageGroupId");

    List<Message> messages = amazonSqs.receiveMessage(messageRequest).getMessages();

    log.info("Received total messages size: {}", messages.size());

    return messages;
  }

}
