package uk.gov.caz.notify.configuration;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AWS client builder configuration.
 */
@Configuration
public class AwsConfiguration {

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * Creates the AmazonSqs Bean with a given region.
   * 
   * @return the AmazonSqs Bean
   */
  @Primary
  @Bean
  public AmazonSQS amazonSqs() {
    AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
    builder.withRegion(region);
    return builder.build();
  }

}
