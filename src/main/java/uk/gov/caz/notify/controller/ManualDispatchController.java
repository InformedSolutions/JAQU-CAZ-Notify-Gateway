package uk.gov.caz.notify.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import uk.gov.caz.notify.domain.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyRepository;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RestController
@RequestMapping(value = "/notify", produces = {
    MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
@Api(value = "/notify")
public class ManualDispatchController {

  private final GovUkNotifyRepository govUkNotifyRepository;

  public ManualDispatchController(GovUkNotifyRepository govUkNotifyRepository) {
    this.govUkNotifyRepository = govUkNotifyRepository;

  }

  @GetMapping("/sendEmail")
  @ApiOperation(value = "Manually dispatch an email",
      response = ResponseEntity.class)
  public SendEmailResponse sendEmail(@RequestBody SendEmailRequest request)
      throws JsonParseException, JsonMappingException,
      NotificationClientException, IOException {
    return govUkNotifyRepository.sendEmail(request.templateId,
        request.emailAddress, request.personalisation, request.reference);
  }
}
