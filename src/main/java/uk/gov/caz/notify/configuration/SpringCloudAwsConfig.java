package uk.gov.caz.notify.configuration;

import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;

@Configuration
public class SpringCloudAwsConfig {

  @Bean
  public QueueMessagingTemplate queueMessagingTemplate(
      AmazonSQSBufferedAsyncClient amazonSQSAsync) {
    return new QueueMessagingTemplate(amazonSQSAsync);
  }

}
