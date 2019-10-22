package uk.gov.caz.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import uk.gov.caz.notify.configuration.RequestMappingConfiguration;
import uk.gov.caz.notify.configuration.SwaggerConfiguration;
import uk.gov.caz.notify.controller.ManualDispatchController;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan({"uk.gov.caz.notify"})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
