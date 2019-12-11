package org.evosuite.coverage.execcount;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.evosuite.ExecutionCountManager;
import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fitness function that looks values test cases based on the execution count of the code they
 * exercise. It can be configured to either value test cases that exercise more often executed code,
 * or those that exercise less often executed code.
 */
public abstract class ExecutionCountCoverageTestFitness extends TestFitnessFunction {

  protected static final Logger logger = LoggerFactory
      .getLogger(ExecutionCountCoverageTestFitness.class);

  /**
   * Used for retrieving information about execution counts for this instance.
   */
  private transient ExecutionCountManager executionCountManager;

  /**
   * Contains line goals only for lines that are executed at least once.
   */
  protected final List<LineCoverageTestFitness> lineGoals;

  /**
   * Constructs this fitness function using the given execution count manager and the given line
   * goals factory.
   */
  protected ExecutionCountCoverageTestFitness(ExecutionCountManager executionCountManager,
      LineCoverageFactory lineFactory) {
    if (lineFactory == null) {
      throw new IllegalArgumentException("lineFactory parameter must be non-null");
    }
    if (executionCountManager == null) {
      throw new IllegalArgumentException("executionCountManager parameter must be non-null");
    }
    this.executionCountManager = executionCountManager;

    logger.debug("Filtering line goals to only contain executed lines");
    this.lineGoals = lineFactory.getCoverageGoals().stream()
        .filter(goal -> executionCountManager.lineExecCount(goal.getLine()) > 0).collect(
            Collectors.toList());
  }

  /**
   * Uses the {@code ExecutionCountManager} to retrieve the weighted average execution count for the
   * lines in this instance.
   *
   * @see org.evosuite.ExecutionCountManager#weightedAvgExecutionCount(Map)
   */
  protected double getWeightedAvgExecCount(TestChromosome individual, ExecutionResult result) {
    Map<Integer, Double> lineToFitness = lineGoals.stream().filter(lineGoal -> individual.getLastExecutionResult().getTrace().getAllCoveredLines().contains(lineGoal.getLine())).collect(Collectors.toMap(
        LineCoverageTestFitness::getLine, line -> line.getFitness(individual, result)));
    return executionCountManager.weightedAvgExecutionCount(lineToFitness);
  }

  @Override
  public int compareTo(TestFitnessFunction other) {
    return compareClassName(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecutionCountCoverageTestFitness that = (ExecutionCountCoverageTestFitness) o;
    return executionCountManager.equals(that.executionCountManager) &&
        lineGoals.equals(that.lineGoals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionCountManager, lineGoals);
  }

  @Override
  public String getTargetClass() {
    return Properties.TARGET_CLASS;
  }

  /**
   * Dummy implementation for this method, because EvoSuite requires one.
   */
  @Override
  public String getTargetMethod() {
    logger
        .warn("Providing target method for a fitness function for which it makes no sense, because "
            + "EvoSuite requires an implementation of 'getTargetMethod()'");
    return "<N/A>";
  }
}
