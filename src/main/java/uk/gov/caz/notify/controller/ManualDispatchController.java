package uk.gov.caz.notify.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.notify.dto.SendEmailRequest;
import uk.gov.caz.notify.repository.GovUkNotifyWrapper;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@RestController
@RequestMapping(value = "/notify", produces = {
    MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE})
@Api(value = "/notify")
public class ManualDispatchController {

  private final GovUkNotifyWrapper govUkNotifyWrapper;

  public ManualDispatchController(GovUkNotifyWrapper govUkNotifyWrapper) {
    this.govUkNotifyWrapper = govUkNotifyWrapper;
  }

  /**
   * A controller method to manually dispatch an email.
   * 
   * @param  request                     containing email, templateId, reference
   *                                       and personalisation
   * @return                             a response entity containing the Gov.UK
   *                                     Notify response
   * @throws NotificationClientException any error that may be thrown by Gov.UK
   *                                       Notify
   */
  @ApiOperation(value = "Manually dispatch an email",
      response = ResponseEntity.class)
  @ApiResponses({
      @ApiResponse(code = 503, message = "External service unavailable"),
      @ApiResponse(code = 500, message = "Internal server error"),
      @ApiResponse(code = 429, message = "Rate limit exceeded"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 200, message = "Email sent"),})
  @GetMapping("/sendEmail")
  public ResponseEntity<SendEmailResponse> sendEmail(
      @RequestBody SendEmailRequest request)
      throws NotificationClientException {
    try {
      SendEmailResponse sendEmailResponse =
          govUkNotifyWrapper.sendEmail(request.templateId,
              request.emailAddress, request.personalisation, request.reference);
      return ResponseEntity.ok(sendEmailResponse);
    } catch (IOException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
