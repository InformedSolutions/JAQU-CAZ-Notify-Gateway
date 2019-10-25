package uk.gov.caz.notify.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
public class GovUkNotifyRepositoryTest {

  GovUkNotifyRepository govUkNotifyRepository;

  @Mock
  NotificationClient client;

  String apiKey = "testApiKey";
  String templateId;
  String emailAddress = "test@notify.com";
  String personalisation = "{\"test\": \"test2\"}";
  String reference;
  SendEmailResponse sendEmailResponse;

  @BeforeEach
  void init() {
    govUkNotifyRepository = new GovUkNotifyRepository(null);
    ReflectionTestUtils.setField(govUkNotifyRepository, "client", client);
    reference = UUID.randomUUID().toString();
    templateId = UUID.randomUUID().toString();
    String response = new JSONObject().put("id", UUID.randomUUID().toString())
        .put("reference", reference)
        .put("content",
            (new JSONObject()).put("body", "testbody").put("subject",
                "testsubject"))
        .put("template", (new JSONObject()).put("id", templateId)
            .put("version", 1).put("uri", "1"))
        .toString();
    sendEmailResponse = new SendEmailResponse(response);
  }

  /**
   * Successful email test
   * 
   * @throws NotificationClientException
   * @throws IOException
   */
  @Test
  public void canSendAnEmail() throws NotificationClientException, IOException {
    Mockito
        .when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyMap(), Mockito.anyString()))
        .thenReturn(sendEmailResponse);

    SendEmailResponse testResponse = govUkNotifyRepository.sendEmail(templateId,
        emailAddress, personalisation, reference);
    assertEquals(templateId, testResponse.getTemplateId().toString());
    assertEquals(reference, testResponse.getReference().get());

  }

  @Test
  public void ioErrorThrown() throws NotificationClientException {
    String personalisation = "{\"testJustKey\"}";
    try {
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (IOException e) {
      assertNotNull(e);
    }
  }

  /**
   * Bad request error test
   * 
   * @throws IOException
   */
  @Test
  public void throwsBadRequestError() throws IOException {
    NotificationClientException err =
        new NotificationClientException("Status code: " + 400 + " " + null);
    try {
      Mockito.when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyMap(), Mockito.anyString())).thenThrow(err);
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (NotificationClientException e) {
      assertEquals("Status code: 400 null", e.getMessage());
    }
  }

  /**
   * Authentication error test
   * 
   * @throws IOException
   */
  @Test
  public void throwsAuthError() throws IOException {
    NotificationClientException err =
        new NotificationClientException("Status code: " + 403 + " " + null);
    try {
      Mockito.when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyMap(), Mockito.anyString())).thenThrow(err);
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (NotificationClientException e) {
      assertEquals("Status code: 403 null", e.getMessage());
    }
  }

  /**
   * Too Many Requests error test
   * 
   * @throws IOException
   */
  @Test
  public void throwsTooManyRequestsError() throws IOException {
    NotificationClientException err =
        new NotificationClientException("Status code: " + 429 + " " + null);
    try {
      Mockito.when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyMap(), Mockito.anyString())).thenThrow(err);
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (NotificationClientException e) {
      assertEquals("Status code: 429 null", e.getMessage());
    }
  }

  /**
   * Internal Server Error test
   * 
   * @throws IOException
   */
  @Test
  public void throwsInternalServerError() throws IOException {
    NotificationClientException err =
        new NotificationClientException("Status code: " + 500 + " " + null);
    try {
      Mockito.when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyMap(), Mockito.anyString())).thenThrow(err);
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (NotificationClientException e) {
      assertEquals("Status code: 500 null", e.getMessage());
    }
  }

  /**
   * Service Unavailable error test
   * 
   * @throws IOException
   */
  @Test
  public void throwsServiceDownError() throws IOException {
    NotificationClientException err =
        new NotificationClientException("Status code: " + 503 + " " + null);
    try {
      Mockito.when(client.sendEmail(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyMap(), Mockito.anyString())).thenThrow(err);
      govUkNotifyRepository.sendEmail(templateId, emailAddress, personalisation,
          reference);
    } catch (NotificationClientException e) {
      assertEquals("Status code: 503 null", e.getMessage());
    }
  }

}
