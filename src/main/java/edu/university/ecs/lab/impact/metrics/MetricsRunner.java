package edu.university.ecs.lab.impact.metrics;

import edu.university.ecs.lab.impact.metrics.services.MetricsService;

import java.io.IOException;
import java.nio.file.Path;

public class MetricsRunner {

  public static void main(String[] args) throws IOException {
    args = new String[]{"./out/rest-extraction-output-[1713387926824].json", "./out/delta-changes-[1713387942758].json"};
    if (args.length < 2) {
      System.err.println(
          "Invalid # of args, 2 expected: <path/to/old/intermediate-json>  <path/to/delta>");
      return;
    }

    MetricsService metricsService =
        new MetricsService(
            Path.of(args[0]).toAbsolutePath().toString(),
            Path.of(args[1]).toAbsolutePath().toString());

    System.out.println(metricsService.generateSystemMetrics());
  }
}
