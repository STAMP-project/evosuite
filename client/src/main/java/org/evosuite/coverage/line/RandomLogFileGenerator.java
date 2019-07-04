package org.evosuite.coverage.line;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomLogFileGenerator {

  private static final Logger logger = LoggerFactory.getLogger(RandomLogFileGenerator.class);

  private List<LineCoverageTestFitness> lineGoals;

  public RandomLogFileGenerator(List<LineCoverageTestFitness> lineGoals) {
    this.lineGoals = lineGoals;
  }

  public String generateUniformRandomLogFile() {
    logger.info("Going to generate random log file entries based on supplied line goals");

    Random random = new Random();
    StringBuilder logStringBuilder = new StringBuilder();

    for (LineCoverageTestFitness goal : lineGoals) {
      int numberOfExecutions = random.nextInt(1000);
      logger.trace("Progress through line goals: " + (lineGoals.indexOf(goal) + 1) + "/" + lineGoals.size());
      logger.trace("Generating " + numberOfExecutions + " log messages for " + goal);

      for (int i = 0; i < numberOfExecutions; i++) {
        logStringBuilder.append(goal.getClassName()).append("|")
            .append(goal.getMethod().split(Pattern.quote("("))[0]).append("|")
            .append(goal.getLine()).append(System.lineSeparator());
      }
    }
    return logStringBuilder.toString();
  }
}
