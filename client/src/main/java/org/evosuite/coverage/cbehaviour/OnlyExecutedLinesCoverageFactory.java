package org.evosuite.coverage.cbehaviour;

import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory supplying line coverage goals only for lines that are present in the
 * execution count JSON file.
 */
@SuppressWarnings("WeakerAccess")
public class OnlyExecutedLinesCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  /**
   * Factory that supplies line goals which are filtered by this class
   */
  private final LineCoverageFactory delegateFactory;
  private final List<ClassExecutionCounts> executionCounts;

  private static final Logger logger =
      LoggerFactory.getLogger(OnlyExecutedLinesCoverageFactory.class);

  /**
   * Constructs a new factory using the execution counts in the list that is provided, and the
   * provided delegate factory that provides the line goals that are filtered by this factory.
   */
  @SuppressWarnings("unused")
  public OnlyExecutedLinesCoverageFactory(List<ClassExecutionCounts> executionCounts,
      LineCoverageFactory delegateFactory) {
    this.delegateFactory = delegateFactory;
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

  /**
   * Returns an instance constructed using the specified JSON count file, and the delegate factory
   * that provides the original list of line goals that the returned factory will filter.
   *
   * @param file a JSON execution count file. It must exist.
   */
  public static OnlyExecutedLinesCoverageFactory fromExecutionCountFile(File file,
      LineCoverageFactory delegateFactory) {
    if (!file.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + file.getAbsolutePath());
    }
    try {
      return new OnlyExecutedLinesCoverageFactory(
          ClassExecutionCounts.readCounts(new Scanner(file).useDelimiter("\\Z").next()),
          delegateFactory);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Just checked if file exists, but not accessible anymore", e);
    } catch (JsonSyntaxException e) {
      logger.error("Execution count file malformed", e);
      throw e;
    }
  }
}
