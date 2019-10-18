package org.evosuite.coverage.execcount;

import org.evosuite.ExecutionCountManager;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test cases that exercise commonly
 * executed code.
 */
public class MaxExecutionCountCoverageTestFitness extends ExecutionCountCoverageTestFitness {

  /**
   * Constructs this fitness function with the given execution count manager and factory for line
   * coverage goals.
   */
  public MaxExecutionCountCoverageTestFitness(ExecutionCountManager executionCountManager,
      LineCoverageFactory lineFactory) {
    super(executionCountManager, lineFactory);
  }

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    return getWeightedAvgExecCount(individual, result);
  }
}
