package org.evosuite.coverage.execcount;

import org.evosuite.ExecutionCountManager;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test suites that do not exercise
 * commonly executed code.
 */
public class MinExecutionCountCoverageSuiteFitness extends ExecutionCountCoverageSuiteFitness {

  /**
   * Constructs this fitness function with the given execution count manager and factory for line
   * coverage goals.
   */
  public MinExecutionCountCoverageSuiteFitness(ExecutionCountManager executionCountManager,
      LineCoverageFactory lineFactory) {
    super(executionCountManager, lineFactory);
  }

  @Override
  public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> individual) {
    double weightedAvgExecCount = getWeightedAvgExecCount(individual);
    return weightedAvgExecCount > 0 ? 1 / weightedAvgExecCount : Double.MAX_VALUE;
  }
}
