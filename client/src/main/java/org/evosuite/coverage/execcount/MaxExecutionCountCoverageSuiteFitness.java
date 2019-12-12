package org.evosuite.coverage.execcount;

import org.evosuite.ExecutionCountManager;
import org.evosuite.coverage.line.LineCoverageFactory;
import org.evosuite.testcase.ExecutableChromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.AbstractTestSuiteChromosome;

/**
 * An {@link ExecutionCountCoverageTestFitness} that prefers test suites that exercise commonly
 * executed code.
 */
public class MaxExecutionCountCoverageSuiteFitness extends ExecutionCountCoverageSuiteFitness {

  /**
   * Constructs this fitness function with the given execution count manager and factory for line
   * coverage goals.
   */
  public MaxExecutionCountCoverageSuiteFitness(ExecutionCountManager executionCountManager,
      LineCoverageFactory lineFactory) {
    super(executionCountManager, lineFactory);
  }

  @Override
  public double getFitness(AbstractTestSuiteChromosome<? extends ExecutableChromosome> individual) {
    return getWeightedAvgExecCount(individual);
  }
}
