package uk.gov.caz.notify.messaging;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;

public class MessagingClient {

  final AmazonSQSAsync sqs;

  public MessagingClient() {
    AmazonSQSAsync sqsAsync = AmazonSQSAsyncClientBuilder.defaultClient();
    sqs = new AmazonSQSBufferedAsyncClient(sqsAsync);
  }

}
