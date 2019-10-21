package org.evosuite.coverage.execcount;

import org.evosuite.ExecutionCountManager;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test cases that do not exercise
 * commonly executed code.
 */
public class MinExecutionCountCoverageTestFitness extends ExecutionCountCoverageTestFitness {

  /**
   * Constructs this fitness function with the given execution count manager and factory for line
   * coverage goals.
   */
  public MinExecutionCountCoverageTestFitness(ExecutionCountManager executionCountManager,
      LineCoverageFactory lineFactory) {
    super(executionCountManager, lineFactory);
  }

  @Override
  public double getFitness(TestChromosome individual, ExecutionResult result) {
    double weightedAvgExecCount = getWeightedAvgExecCount(individual, result);
    return weightedAvgExecCount > 0 ? 1 / weightedAvgExecCount : Double.MAX_VALUE;
  }
}
