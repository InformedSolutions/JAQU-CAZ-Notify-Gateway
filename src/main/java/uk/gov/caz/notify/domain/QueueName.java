package uk.gov.caz.notify.domain;

public enum QueueName {
  NEW("new"), REQUEST_LIMIT("request-limit"), SERVICE_ERROR(
      "service-error"), SERVICE_DOWN("service-down");

  private final String text;

  QueueName(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return this.text;
  }
}
