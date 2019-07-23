package org.evosuite.coverage.line;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates random log files using supplied line goals. It does not create actual files itself, but
 * outputs the would-be contents as a string.
 */
public class RandomLogFileGenerator {

  private static final Logger logger = LoggerFactory.getLogger(RandomLogFileGenerator.class);

  /**
   * The line goals representing all lines in the target class, which are used to generate random
   * log file.
   */
  private List<LineCoverageTestFitness> lineGoals;

  /**
   * Constructs a new instance using the specified line goals for generating the random log file.
   */
  public RandomLogFileGenerator(List<LineCoverageTestFitness> lineGoals) {
    this.lineGoals = lineGoals;
  }

  /**
   * Generates a random log file, returning it as a string. For each line that is supplied, a random
   * amount of log messages are generated.
   */
  public String generateUniformRandomLogFile() {
    logger.info("Going to generate random log file entries based on supplied line goals");

    Random random = new Random();
    StringBuilder logStringBuilder = new StringBuilder();

    for (int i = 0; i < lineGoals.size(); i++) {
      LineCoverageTestFitness goal = lineGoals.get(i);
      int numberOfExecutions = random.nextInt(1000);
      logger.trace("Progress through line goals: " + (i + 1) + "/" + lineGoals.size());
      logger.trace("Generating " + numberOfExecutions + " log messages for " + goal);

      for (int j = 0; j < numberOfExecutions; j++) {
        logStringBuilder.append(goal.getClassName()).append("|")
            .append(goal.getMethod().split(Pattern.quote("("))[0]).append("|")
            .append(goal.getLine()).append(System.lineSeparator());
      }
    }
    return logStringBuilder.toString();
  }
}
