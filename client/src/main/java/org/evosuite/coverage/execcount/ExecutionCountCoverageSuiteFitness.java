package org.evosuite.coverage.execcount;

import org.evosuite.testsuite.TestSuiteFitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fitness function that looks values test suites based on the execution count of the code they
 * exercise. It can be configured to either value test suites that exercise more often executed code,
 * or those that exercise less often executed code.
 */

public abstract class ExecutionCountCoverageSuiteFitness extends TestSuiteFitnessFunction {

  protected static final Logger logger = LoggerFactory
      .getLogger(ExecutionCountCoverageSuiteFitness.class);

}
