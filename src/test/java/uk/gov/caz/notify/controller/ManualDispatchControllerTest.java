package uk.gov.caz.notify.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.notify.domain.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyRepository;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class ManualDispatchControllerTest {

  @InjectMocks
  ManualDispatchController manualDispatchController;

  @Mock
  GovUkNotifyRepository govUkNotifyRepository;

  SendEmailRequest sendEmailRequest;
  SendEmailResponse sendEmailResponse;

  String response;
  String templateId;
  String reference;

  @BeforeEach
  void init() {

    sendEmailRequest = new SendEmailRequest();
    templateId = UUID.randomUUID().toString();
    reference = UUID.randomUUID().toString();

    response = new JSONObject().put("id", UUID.randomUUID().toString())
        .put("reference", reference)
        .put("content",
            (new JSONObject()).put("body", "testbody").put("subject",
                "testsubject"))
        .put("template", (new JSONObject()).put("id", templateId)
            .put("version", 1).put("uri", "1"))
        .toString();

    sendEmailRequest.emailAddress = "test@test.com";
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = "{}";
    sendEmailRequest.reference = reference;

    sendEmailResponse = new SendEmailResponse(response);
  }

  @Test
  void canSendEmail() throws NotificationClientException, IOException {
    Mockito.when(govUkNotifyRepository.sendEmail(templateId,
        sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
        reference)).thenReturn(sendEmailResponse);

    ResponseEntity<SendEmailResponse> testResponse =
        manualDispatchController.sendEmail(sendEmailRequest);
    assertEquals(sendEmailResponse, testResponse.getBody());
  }

  @Test
  void canGetBadRequestError() throws NotificationClientException, IOException {
    Mockito.when(govUkNotifyRepository.sendEmail(templateId,
        sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
        reference)).thenThrow(new IOException());

    ResponseEntity<SendEmailResponse> testResponse =
        manualDispatchController.sendEmail(sendEmailRequest);
    assertEquals(400, testResponse.getStatusCode().value());
  }

}
