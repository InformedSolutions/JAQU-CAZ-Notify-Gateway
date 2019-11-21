package uk.gov.caz.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageConsumerRequest {

  String queueName;

}
