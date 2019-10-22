package uk.gov.caz.notify.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

  @Test
  void canSendEmail() throws JsonParseException, JsonMappingException,
      NotificationClientException, IOException {
    SendEmailRequest sendEmailRequest = new SendEmailRequest();
    String templateId = UUID.randomUUID().toString();
    String reference = UUID.randomUUID().toString();
    sendEmailRequest.emailAddress = "test@test.com";
    sendEmailRequest.templateId = templateId;
    sendEmailRequest.personalisation = "{}";
    sendEmailRequest.reference = reference;


    String response = new JSONObject().put("id", UUID.randomUUID().toString())
        .put("reference", reference)
        .put("content",
            (new JSONObject()).put("body", "testbody").put("subject",
                "testsubject"))
        .put("template", (new JSONObject()).put("id", templateId)
            .put("version", 1).put("uri", "1"))
        .toString();
    SendEmailResponse sendEmailResponse = new SendEmailResponse(response);

    Mockito.when(govUkNotifyRepository.sendEmail(templateId,
        sendEmailRequest.emailAddress, sendEmailRequest.personalisation,
        reference)).thenReturn(sendEmailResponse);

    SendEmailResponse testResponse =
        manualDispatchController.sendEmail(sendEmailRequest);
    assertEquals(sendEmailResponse, testResponse);


  }

}
