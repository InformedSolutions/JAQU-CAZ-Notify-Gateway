package uk.gov.caz.notify.dto;

import lombok.Data;

@Data
public class SendEmailRequest {

  public String templateId;
  public String emailAddress;
  public String personalisation;
  public String reference;

}
