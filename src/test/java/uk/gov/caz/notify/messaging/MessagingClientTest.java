package uk.gov.caz.notify.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;

@ExtendWith(MockitoExtension.class)
public class MessagingClientTest {

  @InjectMocks
  MessagingClient messagingClient;

  @Mock
  GovUkNotifyWrapper govUkNotifyWrapper;

  @Mock
  QueueMessagingTemplate messagingTemplate;

  @Mock
  NotificationClientException err;

  Map<String, Object> headers;
  String message;
  SendEmailRequest sendEmailRequest;
  String emailAddress;
  String templateId;
  String personalisation;
  String reference;

  ObjectMapper om;


  @BeforeEach
  void init() throws JsonProcessingException {
    sendEmailRequest = new SendEmailRequest();
    templateId = UUID.randomUUID().toString();
    reference = UUID.randomUUID().toString();
    emailAddress = "test@test.com";
    personalisation = "{}";

    sendEmailRequest.emailAddress = emailAddress;
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = personalisation;
    sendEmailRequest.reference = reference;

    headers = new HashMap<String, Object>();
    headers.put("MessageGroupId", "test");

    ReflectionTestUtils.setField(messagingClient, "deadLetterQueue", "dlq");
    ReflectionTestUtils.setField(messagingClient, "requestLimitQueue",
        "requestLimit");
    ReflectionTestUtils.setField(messagingClient, "serviceDownQueue",
        "serviceDown");
    ReflectionTestUtils.setField(messagingClient, "serviceErrorQueue",
        "serviceError");
  }

  @Test
  void canSendBadRequestErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(400);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("dlq"), ArgumentMatchers.eq(sendEmailRequest),
        Mockito.anyMap());

  }

  @Test
  void canSendAuthorizationErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(403);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("dlq"), ArgumentMatchers.eq(sendEmailRequest),
        Mockito.anyMap());

  }

  @Test
  void canSendRequestLimitErrorToRequestLimitQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(429);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("requestLimit"),
        ArgumentMatchers.eq(sendEmailRequest), Mockito.anyMap());

  }

  @Test
  void canSendServiceErrorToServiceErrorQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(500);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("serviceError"),
        ArgumentMatchers.eq(sendEmailRequest), Mockito.anyMap());

  }

  @Test
  void canSendServiceDownToServiceDownQueue()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(503);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("serviceDown"),
        ArgumentMatchers.eq(sendEmailRequest), Mockito.anyMap());

  }

  @Test
  void canSendUnexpectedErrorToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    Mockito.when(err.getHttpResult()).thenReturn(418);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("dlq"), ArgumentMatchers.eq(sendEmailRequest),
        Mockito.anyMap());

  }

  @Test
  void canSendBadlyFormedParametersToDlq()
      throws NotificationClientException, IOException, InstantiationException {

    this.sendEmailRequest.personalisation = "{";
    IOException err = mock(IOException.class);
    Mockito.when(err.getMessage()).thenReturn("Error thrown successfully.");

    Mockito.when(
        govUkNotifyWrapper.sendEmail(templateId, emailAddress, "{", reference))
        .thenThrow(err);

    messagingClient.handleMessage(sendEmailRequest, headers);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, "{", reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        ArgumentMatchers.eq("dlq"), ArgumentMatchers.eq(sendEmailRequest),
        Mockito.anyMap());

  }

}
