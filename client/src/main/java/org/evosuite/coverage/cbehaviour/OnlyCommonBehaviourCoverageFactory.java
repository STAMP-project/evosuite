package org.evosuite.coverage.cbehaviour;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing logic to supply line coverage goals only for lines that are present in the
 * execution count JSON file.
 */
@SuppressWarnings("WeakerAccess")
public class OnlyCommonBehaviourCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  private final LineCoverageFactory delegateFactory;
  private final List<ClassExecutionCounts> executionCounts;

  private static final Logger logger =
      LoggerFactory.getLogger(OnlyCommonBehaviourCoverageFactory.class);

  /**
   * Constructs a new factory using the execution counts in the list that is provided.
   */
  @SuppressWarnings("unused")
  public OnlyCommonBehaviourCoverageFactory(List<ClassExecutionCounts> executionCounts) {
    delegateFactory = new LineCoverageFactory();
    this.executionCounts = executionCounts;
  }

  /**
   * Supplies line coverage goals only for lines that appear in the input execution count JSON file,
   * of which the contents have been provided upon construction of this object.
   */
  @Override
  public List<LineCoverageTestFitness> getCoverageGoals() {
    logger.info("Generating coverage goals for executed lines");
    List<LineCoverageTestFitness> allLines = delegateFactory.getCoverageGoals();
    logger.trace("All lines in class: " + allLines);

    logger.trace("Using execution counts: " + executionCounts);
    List<LineCoverageTestFitness> executedLines =
        CommonBehaviourUtil.retainExecutedLines(allLines, executionCounts);
    logger.trace("Executed lines: " + executedLines);
    return executedLines;
  }

  public static OnlyCommonBehaviourCoverageFactory fromExecutionCountFile(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new OnlyCommonBehaviourCoverageFactory(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next()));
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw e;
    }
  }
}
