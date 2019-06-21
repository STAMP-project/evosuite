package org.evosuite.coverage.cbehaviour;

import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test cases that exercise commonly
 * executed code.
 */
public class HighExecutionCountCoverageTestFitness extends ExecutionCountCoverageTestFitness {
  /**
   * Constructs this fitness function with the given execution counts and factory for line coverage
   * goals.
   *
   * @param executionCounts the execution counts containing counts for the class under test
   */
  public HighExecutionCountCoverageTestFitness(ClassExecutionCounts executionCounts,
      LineCoverageFactory lineFactory) {
    super(executionCounts, lineFactory);
  }

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    return lineGoals.stream().mapToDouble(goal -> goal.getFitness(individual, result) *
        getExecutionCount(goal)).sum()
        + Double.MIN_VALUE; //This is added so the goal is never satisfied, which is most often
    // what one wants when using this goal.
  }
}
