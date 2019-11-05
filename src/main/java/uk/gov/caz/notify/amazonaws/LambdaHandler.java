package uk.gov.caz.notify.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.notify.Application;

public class LambdaHandler {

  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  public void eventHandler(ScheduledEvent scheduledEvent) {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }
  }
}
