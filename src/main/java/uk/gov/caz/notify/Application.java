package uk.gov.caz.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan({"uk.gov.caz.notify"})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
