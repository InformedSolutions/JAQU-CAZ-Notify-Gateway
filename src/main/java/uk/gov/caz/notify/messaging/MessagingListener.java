package uk.gov.caz.notify.messaging;

import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class MessagingListener {

  /**
   * A listener method to consume messages from the 'new' message queue.
   * 
   * @param payload the content of the message
   */
  @SqsListener(value = "${application.queue.new}",
      deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
  public void consumeMessage(String payload) {
    // try
    System.out.println(payload);
    // catch 400 & 403 errors -> DLQ
    // catch 429 errors -> request limit queue
    // catch 500 errors -> service error queuerror -> DLQ
  }
}
