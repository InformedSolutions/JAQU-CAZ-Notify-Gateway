package uk.gov.caz.notify.messaging;

import static org.mockito.Mockito.times;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.caz.notify.dto.NotifyErrorResponse;
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

  SendEmailRequest sendEmailRequest;
  String emailAddress;
  String templateId;
  String personalisation;
  String reference;


  @BeforeEach
  void init() {
    sendEmailRequest = new SendEmailRequest();
    templateId = UUID.randomUUID().toString();
    reference = UUID.randomUUID().toString();

    sendEmailRequest.emailAddress = emailAddress;
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = personalisation;
    sendEmailRequest.reference = reference;

    ReflectionTestUtils.setField(messagingClient, "dlq", "dlq");
    ReflectionTestUtils.setField(messagingClient, "requestLimit",
        "requestLimit");
    ReflectionTestUtils.setField(messagingClient, "serviceDown", "serviceDown");
    ReflectionTestUtils.setField(messagingClient, "serviceError",
        "serviceError");
  }

  @Test
  void canSendBadRequestErrorToDlq()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(400);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("dlq"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendAuthorizationErrorToDlq()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(403);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("dlq"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendRequestLimitErrorToRequestLimitQueue()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(429);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("requestLimit"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendServiceErrorToServiceErrorQueue()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(500);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("serviceError"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendServiceDownToServiceDownQueue()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(503);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("serviceDown"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendUnexpectedErrorToDlq()
      throws NotificationClientException, IOException {

    Mockito.when(err.getHttpResult()).thenReturn(418);

    Mockito.when(govUkNotifyWrapper.sendEmail(templateId, emailAddress,
        personalisation, reference)).thenThrow(err);

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(4)).sendEmail(templateId,
        emailAddress, personalisation, reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("dlq"), Mockito.any(NotifyErrorResponse.class));

  }

  @Test
  void canSendBadlyFormedParametersToDlq()
      throws NotificationClientException, IOException {

    this.sendEmailRequest.personalisation = "{";

    Mockito.when(
        govUkNotifyWrapper.sendEmail(templateId, emailAddress, "{", reference))
        .thenThrow(new IOException());

    messagingClient.consumeMessage(sendEmailRequest);

    Mockito.verify(govUkNotifyWrapper, times(1)).sendEmail(templateId,
        emailAddress, "{", reference);
    Mockito.verify(messagingTemplate, times(1)).convertAndSend(
        Mockito.eq("dlq"), Mockito.any(NotifyErrorResponse.class));

  }

}
