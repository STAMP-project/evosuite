package org.evosuite.coverage.execcount;

import java.util.List;
import java.util.stream.Collectors;
import org.evosuite.ExecutionCountManager;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory supplying line coverage goals only for lines that are executed at least once according to
 * the {@link ExecutionCountManager} provided upon construction.
 */
@SuppressWarnings("WeakerAccess")
public class OnlyExecutedLinesCoverageFactory extends
    AbstractFitnessFactory<LineCoverageTestFitness> {

  /**
   * Factory that supplies line goals which are filtered by this class depending on whether they
   * have been executed
   */
  private final LineCoverageFactory delegateFactory;

  /**
   * Manager that is used to determine whether a certain line has been executed or not
   */
  private final transient ExecutionCountManager executionCountManager;

  private static final Logger logger =
      LoggerFactory.getLogger(OnlyExecutedLinesCoverageFactory.class);

  /**
   * Constructs a new factory using the execution count manager that is provided, and the provided
   * delegate factory that provides the line goals that are filtered by this factory.
   */
  @SuppressWarnings("unused")
  public OnlyExecutedLinesCoverageFactory(
      ExecutionCountManager executionCountManager,
      LineCoverageFactory delegateFactory) {
    this.executionCountManager = executionCountManager;
    this.delegateFactory = delegateFactory;
  }

  /**
   * Supplies line coverage goals only for lines that have been executed
   */
  @Override
  public List<LineCoverageTestFitness> getCoverageGoals() {
    logger.info("Generating coverage goals for executed lines");
    List<LineCoverageTestFitness> allLines = delegateFactory.getCoverageGoals();
    logger.trace("All lines in class: " + allLines);

    logger.trace("Using execution count manager: " + executionCountManager);
    List<LineCoverageTestFitness> executedLines = allLines.stream()
        .filter(goal -> executionCountManager.lineExecCount(goal.getLine()) > 0).collect(
            Collectors.toList());
    logger.trace("Executed lines: " + executedLines);
    return executedLines;
  }
}
