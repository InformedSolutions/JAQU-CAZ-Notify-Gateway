package uk.gov.caz.notify.configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS client builder configuration.
 */
@Configuration
public class SpringCloudAwsConfig {

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * Creates the AmazonSqsAsync Bean. Overriding the default SQS Client Bean
   * config because AmazonSqsBufferedAsyncClient is not currently supported by
   * FIFO queues.
   * 
   * @return the AmazonSqsAsync Bean
   */
  @Bean
  public AmazonSQSAsync amazonSqs() {

    AmazonSQSAsyncClientBuilder builder =
        AmazonSQSAsyncClientBuilder.standard();
    builder.withRegion(region);
    return builder.build();
  }

  @Bean
  public QueueMessagingTemplate queueMessagingTemplate(
      AmazonSQSAsync amazonSqs) {
    return new QueueMessagingTemplate(amazonSqs);
  }

}
