package uk.gov.caz.notify.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;

@ExtendWith(MockitoExtension.class)
public class MessagingClientTest {

  MessagingClient messagingClient;

  @Mock
  AmazonSQS client;

  @Mock
  GovUkNotifyWrapper govUkNotifyWrapper;

  @Mock
  NotificationClientException err;

  String messageGroupId;
  String message;
  SendEmailRequest sendEmailRequest;
  String emailAddress;
  String templateId;
  String personalisation;
  String reference;

  ObjectMapper om;


  @BeforeEach
  void init() throws JsonProcessingException {
    messagingClient = new MessagingClient(client, govUkNotifyWrapper, new ObjectMapper());

    sendEmailRequest = new SendEmailRequest();
    templateId = UUID.randomUUID().toString();
    reference = UUID.randomUUID().toString();
    emailAddress = "test@test.com";
    personalisation = "{}";

    sendEmailRequest.emailAddress = emailAddress;
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = personalisation;
    sendEmailRequest.reference = reference;

    messageGroupId = "testMsgGroupId";

    ReflectionTestUtils.setField(messagingClient, "deadLetterQueue", "dlq");
    ReflectionTestUtils.setField(messagingClient, "requestLimitQueue", "requestLimit");
    ReflectionTestUtils.setField(messagingClient, "serviceDownQueue", "serviceDown");
    ReflectionTestUtils.setField(messagingClient, "serviceErrorQueue", "serviceError");

    GetQueueUrlResult getQueueUrlResult = new GetQueueUrlResult();
    getQueueUrlResult.setQueueUrl("testurl");
    Mockito.when(client.getQueueUrl(Mockito.anyString())).thenReturn(getQueueUrlResult);
  }

  @Test
  void canSendBadRequestErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(400);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendAuthorizationErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(403);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendRequestLimitErrorToRequestLimitQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(429);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendServiceErrorToServiceErrorQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(500);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendServiceDownToServiceDownQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(503);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendUnexpectedErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(418);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, personalisation, reference))
        .thenThrow(err);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId, emailAddress,
        personalisation, reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

  @Test
  void canSendBadlyFormedParametersToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    this.sendEmailRequest.personalisation = "{";
    IOException err = mock(IOException.class);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress, "{", reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, messageGroupId);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId, emailAddress, "{",
        reference);
    Mockito.verify(client, times(1)).sendMessage(Mockito.any(SendMessageRequest.class));
  }

}
