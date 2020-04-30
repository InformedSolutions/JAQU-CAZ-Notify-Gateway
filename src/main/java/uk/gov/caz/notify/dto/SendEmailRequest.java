package uk.gov.caz.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailRequest {

  private String templateId;
  private String emailAddress;
  private String personalisation;
  private String reference;

}
