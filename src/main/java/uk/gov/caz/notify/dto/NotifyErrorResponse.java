package uk.gov.caz.notify.dto;

import java.util.UUID;
import com.amazonaws.services.sqs.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

public class NotifyErrorResponse {

  public int statusCode;
  public String message;
  public SendEmailRequest sendEmailRequest;
  private String MessageGroupId;

  public NotifyErrorResponse(int statusCode, String message,
      SendEmailRequest sendEmailRequest) {
    this.statusCode = statusCode;
    this.message = message;
    this.sendEmailRequest = sendEmailRequest;
    this.MessageGroupId = UUID.randomUUID().toString();
  }

}
