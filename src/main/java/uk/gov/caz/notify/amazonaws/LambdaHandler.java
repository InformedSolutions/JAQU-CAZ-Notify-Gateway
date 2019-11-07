package uk.gov.caz.notify.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.notify.Application;
import uk.gov.caz.notify.dto.MessageConsumerRequest;
import uk.gov.caz.notify.service.MessageHandlingService;

@Slf4j
public class LambdaHandler implements RequestStreamHandler {

  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  private MessageHandlingService messageHandlingService;

  private ObjectMapper obj = new ObjectMapper();

  @Override
  public void handleRequest(InputStream input, OutputStream output,
      Context context) throws IOException {

    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
    }

    messageHandlingService =
        this.getBean(handler, MessageHandlingService.class);

    try {
      log.info("Deserializing input object");
      MessageConsumerRequest request =
          obj.readValue(input, MessageConsumerRequest.class);
      log.info("Processing messages from the {} queue", request.getQueueName());
      messageHandlingService.sendQueuedMessages(request.getQueueName());
    } catch (JsonMappingException jme) {
      log.error("Failed to deserialize input object: {}", input.toString());
      log.error(jme.getMessage());
    }

  }

  /**
   * Private helper for instantiating a Bean from the application.
   */
  private <T> T getBean(
      SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext())
        .getBean(exampleServiceClass);
  }
}
