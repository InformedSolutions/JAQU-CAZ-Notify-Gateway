package uk.gov.caz.notify.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.messaging.MessagingClient;

@ExtendWith(MockitoExtension.class)
public class MessageHandlingServiceTest {

  @InjectMocks
  MessageHandlingService messageHandlingService;

  @Mock
  AmazonSQS amazonSqs;

  @Mock
  MessagingClient messagingClient;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void canInstantiateMessageHandlingService() {
    MessageHandlingService messageHandlingService =
        new MessageHandlingService(amazonSqs, messagingClient);
    assertNotNull(messageHandlingService);
  }

  @Test
  void canReceiveMessages()
      throws JsonProcessingException, InstantiationException {

    // set up messages
    SendEmailRequest ser = new SendEmailRequest("testTemplate", "testEmail",
        "testPersonalisation", "testReference");
    String msgBody = objectMapper.writeValueAsString(ser);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("MessageGroupId", "testId");

    Map<String, Object> newHeaders = new HashMap<String, Object>();
    newHeaders.put(SqsMessageHeaders.SQS_GROUP_ID_HEADER, "testId");

    Message msg1 = new Message();
    msg1.setBody(msgBody);
    msg1.setAttributes(headers);

    Message msg2 = new Message();
    msg2.setBody(msgBody);
    msg2.setAttributes(headers);

    List<Message> msgList = new ArrayList<Message>();
    msgList.add(msg1);
    msgList.add(msg2);

    GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
    getQueueUrlResult.setQueueUrl("testUrl");

    ReceiveMessageResult rmr = new ReceiveMessageResult();
    rmr.setMessages(msgList);

    Mockito
        .when(
            amazonSqs.receiveMessage(Mockito.any(ReceiveMessageRequest.class)))
        .thenReturn(rmr);
    Mockito.when(amazonSqs.getQueueUrl("test")).thenReturn(getQueueUrlResult);
    Mockito.when(messagingClient.filterHeaders(headers)).thenReturn(newHeaders);

    messageHandlingService.sendQueuedMessages("test");

    // assertions
    Mockito.verify(amazonSqs, times(1))
        .receiveMessage(Mockito.any(ReceiveMessageRequest.class));
    Mockito.verify(messagingClient, times(2))
        .handleMessage(Mockito.any(SendEmailRequest.class), Mockito.anyMap());

  }

}
