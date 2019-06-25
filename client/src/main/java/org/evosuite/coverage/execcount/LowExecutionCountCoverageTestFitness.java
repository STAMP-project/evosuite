package org.evosuite.coverage.execcount;

import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test cases that do not exercise
 * commonly executed code.
 */
public class LowExecutionCountCoverageTestFitness extends
    ExecutionCountCoverageTestFitness {

  /**
   * Constructs this fitness function with the given execution counts and factory for line coverage
   * goals.
   *
   * @param executionCounts the execution counts containing counts for the class under test
   */
  public LowExecutionCountCoverageTestFitness(ClassExecutionCounts executionCounts,
      LineCoverageFactory lineFactory) {
    super(executionCounts, lineFactory);
  }

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    double weightedSum = lineGoals.stream()
        .mapToDouble(goal -> goal.getFitness(individual, result) *
            getExecutionCount(goal)).sum();
    if (weightedSum == 0) {
      return Double.MAX_VALUE; //To prevent division by 0
    }
    return 1 / weightedSum;
  }
}
